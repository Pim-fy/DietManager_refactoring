package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.LoginFailure;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.LoginFailureRepository;
import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

/**
 * 로그인 실패 제한. 10분 안에 계정별 5회, 접속 주소별 30회를 넘으면 막는다.
 * 실패 횟수는 DB(login_failures)에 저장해 서버를 재시작해도 유지된다.
 * 이메일·IP는 SHA-256 해시로만 저장한다.
 */
@Component
public class LoginAttemptLimiter {
    public static class Limited extends BusinessException {
        public Limited() {
            super(ErrorCode.LOGIN_LIMIT);
        }
    }

    static final long WINDOW_SECONDS = 600;
    static final int ACCOUNT_LIMIT = 5;
    static final int IP_LIMIT = 30;

    private final LoginFailureRepository failures;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public LoginAttemptLimiter(LoginFailureRepository failures, PlatformTransactionManager transactionManager, Clock clock) {
        this.failures = failures;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public void check(String ip, String email) {
        LocalDateTime windowStart = now().minusSeconds(WINDOW_SECONDS);
        if (count(ipKey(ip), windowStart) >= IP_LIMIT || count(accountKey(email), windowStart) >= ACCOUNT_LIMIT)
            throw new Limited();
    }

    // 같은 키를 동시에 올릴 때 횟수가 빠지지 않도록 프로세스 안에서는 한 번에 하나씩 처리하고,
    // 트랜잭션은 잠금 안에서 커밋한다.
    public synchronized void failed(String ip, String email) {
        transaction.executeWithoutResult(status -> {
            increment(ipKey(ip));
            increment(accountKey(email));
        });
    }

    public synchronized void succeeded(String email) {
        transaction.executeWithoutResult(status -> failures.deleteById(accountKey(email)));
    }

    /** 제한 시간이 지난 기록을 10분마다 정리한다. */
    @Scheduled(fixedDelay = WINDOW_SECONDS * 1000, initialDelay = WINDOW_SECONDS * 1000)
    public void cleanup() {
        transaction.executeWithoutResult(status -> failures.deleteExpired(now().minusSeconds(WINDOW_SECONDS)));
    }

    private int count(String key, LocalDateTime windowStart) {
        return failures.findById(key).filter(failure -> failure.isActive(windowStart)).map(LoginFailure::getCount).orElse(0);
    }

    private void increment(String key) {
        LocalDateTime now = now();
        LoginFailure failure = failures.findById(key).orElseGet(() -> new LoginFailure(key, now));
        failure.increment(now, now.minusSeconds(WINDOW_SECONDS));
        failures.save(failure);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    private String accountKey(String email) {
        return "account:" + sha256(email.trim().toLowerCase(Locale.ROOT));
    }

    private String ipKey(String ip) {
        return "ip:" + sha256(ip);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 없음");
        }
    }
}
