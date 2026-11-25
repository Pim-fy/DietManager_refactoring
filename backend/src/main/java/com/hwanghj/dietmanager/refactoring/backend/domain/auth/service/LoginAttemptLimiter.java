package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.LoginFailure;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.LoginFailureRepository;
import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

/**
 * 로그인 시도 제한. 10분 안에 계정·주소 조합별 5회, 계정별 50회, 접속 주소별 30회까지 비밀번호를 검사한다.
 *
 * <ol>
 * <li>{@link #attempt}: 비밀번호 검사 전에 각 제한 기록 행을 잠그고, 한도 안이면 시도를 1 늘려 바로 커밋한다.
 * 동시 요청은 행 잠금 때문에 줄을 서고, 앞 요청이 늘린 횟수를 보고 판단한다.</li>
 * <li>{@link #succeeded}: 로그인에 성공하면 계정과 해당 계정·주소 조합 기록을 지우고 주소 시도를 1 되돌린다(주소 횟수 = 실패 횟수).</li>
 * </ol>
 *
 * 키는 이메일·IP를 서버 비밀키로 만든 HMAC-SHA256으로 저장한다(키가 없으면 되돌릴 수 없음).
 * 확실하지 않은 상황(처음 저장 재시도 실패, 잠금 대기 3초 초과)은 막는다(429).
 */
@Component
public class LoginAttemptLimiter {
    public static class Limited extends BusinessException {
        private final long retryAfterSeconds;

        public Limited(long retryAfterSeconds) {
            super(ErrorCode.LOGIN_LIMIT);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        @Override
        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    static final long WINDOW_SECONDS = 600;
    static final int ACCOUNT_IP_LIMIT = 5;
    static final int ACCOUNT_LIMIT = 50;
    static final int IP_LIMIT = 30;

    private static final String ACCOUNT = "account:";
    private static final String ACCOUNT_IP = "account-ip:";
    private static final String IP = "ip:";

    private final LoginFailureRepository failures;
    private final TransactionTemplate transaction;
    private final TransactionTemplate newTransaction;
    private final Clock clock;
    private final SecretKeySpec hmacKey;

    public LoginAttemptLimiter(LoginFailureRepository failures, PlatformTransactionManager transactionManager, Clock clock,
            @Value("${login-limit.hmac-key}") String hmacKey) {
        if (hmacKey == null || hmacKey.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("login-limit.hmac-key는 32바이트 이상이어야 합니다.");
        this.failures = failures;
        this.transaction = new TransactionTemplate(transactionManager);
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
        this.hmacKey = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** 비밀번호 검사 전에 호출한다. 한도를 넘었으면 {@link Limited}, 아니면 시도를 1 늘린다. */
    public void attempt(String ip, String email) {
        var limits = limits(ip, email);
        // 정리 작업이 만든 직후 행을 지웠거나 version이 충돌한 드문 경우를 위해 한 번 더 시도하고, 그래도 안 되면 막는다.
        for (int round = 0; round < 2; round++) {
            LocalDateTime now = now();
            // MySQL은 없는 행을 잠금 조회하면 갭 잠금을 걸어 이후 INSERT가 막힌다.
            // 그래서 잠그기 전에 행을 먼저 만들어 두고, 잠금은 이미 있는 행에만 건다.
            for (String key : limits.keySet())
                ensureExists(key, now);
            if (lockCheckAndCount(limits, now))
                return;
        }
        throw new Limited(WINDOW_SECONDS);
    }

    /** 행을 잠그고 한도를 확인한 뒤 시도를 1 늘린다. 행이 사라졌거나 version이 충돌했으면 false. */
    private boolean lockCheckAndCount(TreeMap<String, Integer> limits, LocalDateTime now) {
        try {
            Boolean counted = transaction.execute(status -> {
                LocalDateTime windowStart = now.minusSeconds(WINDOW_SECONDS);
                List<LoginFailure> locked = new ArrayList<>();
                // 교착을 막기 위해 항상 키 문자열 순서로 잠근다(TreeMap 순서).
                for (String key : limits.keySet()) {
                    var failure = failures.findWithLockByKey(key);
                    if (failure.isEmpty())
                        return false;
                    locked.add(failure.get());
                }
                long retryAfter = 0;
                for (LoginFailure failure : locked) {
                    if (failure.activeCount(windowStart) >= limits.get(failure.getKey()))
                        retryAfter = Math.max(retryAfter, failure.secondsUntilReset(now, WINDOW_SECONDS));
                }
                if (retryAfter > 0)
                    throw new Limited(retryAfter);
                for (LoginFailure failure : locked)
                    failure.increment(now, windowStart);
                return true;
            });
            return Boolean.TRUE.equals(counted);
        } catch (OptimisticLockingFailureException changed) {
            // 잠금 조회가 최신 값을 보지 못한 경우(H2에서 발생) version 검사로 저장이 거부된다. 세지 않았으므로 다시 시도한다.
            return false;
        } catch (PessimisticLockingFailureException | QueryTimeoutException error) {
            // 같은 키에 요청이 몰려 3초 안에 잠금을 얻지 못했다.
            throw new Limited(WINDOW_SECONDS);
        }
    }

    /** 키 행이 없으면 새 트랜잭션에서 만든다. 동시에 다른 요청이 만들었으면(중복키) 그대로 둔다. */
    private void ensureExists(String key, LocalDateTime now) {
        try {
            newTransaction.executeWithoutResult(status -> {
                if (!failures.existsById(key))
                    failures.saveAndFlush(new LoginFailure(key, now));
            });
        } catch (DataIntegrityViolationException alreadyCreated) {
            // 다른 요청이 먼저 만들었다.
        }
    }

    /** 로그인에 성공했을 때 호출한다. */
    public void succeeded(String ip, String email) {
        String ipKey = ipKey(ip);
        transaction.executeWithoutResult(status -> {
            for (String key : limits(ip, email).keySet()) {
                failures.findWithLockByKey(key).ifPresent(failure -> {
                    if (key.equals(ipKey))
                        failure.decrement();
                    else
                        failures.delete(failure);
                });
            }
        });
    }

    /** 제한 시간이 지난 기록을 10분마다 묶음으로 정리한다. */
    @Scheduled(fixedDelay = WINDOW_SECONDS * 1000, initialDelay = WINDOW_SECONDS * 1000)
    public int cleanup() {
        LocalDateTime windowStart = now().minusSeconds(WINDOW_SECONDS);
        int deleted = 0;
        while (true) {
            try {
                int batchSize = newTransaction.execute(status -> {
                    List<LoginFailure> expired = failures.findTop500ByStartedAtLessThanEqualOrderByKey(windowStart);
                    for (LoginFailure failure : expired)
                        failures.delete(failure);
                    return expired.size();
                });
                if (batchSize == 0)
                    return deleted;
                deleted += batchSize;
            } catch (OptimisticLockingFailureException changed) {
                // 묶음을 롤백하고 다시 조회한다. 로그인으로 새 기간이 시작된 행은 다음 조회에서 빠진다.
            }
        }
    }

    /** 키 → 한도. TreeMap이라 키 문자열 순서로 정렬된다. */
    private TreeMap<String, Integer> limits(String ip, String email) {
        var limits = new TreeMap<String, Integer>();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        limits.put(ACCOUNT + hmac(normalizedEmail), ACCOUNT_LIMIT);
        limits.put(ACCOUNT_IP + hmac(ip + "\0" + normalizedEmail), ACCOUNT_IP_LIMIT);
        limits.put(ipKey(ip), IP_LIMIT);
        return limits;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    private String ipKey(String ip) {
        return IP + hmac(ip);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException error) {
            throw new IllegalStateException("HMAC-SHA256을 사용할 수 없습니다.");
        }
    }
}
