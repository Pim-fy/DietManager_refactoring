package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.LoginFailure;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.LoginFailureRepository;
import com.hwanghj.dietmanager.refactoring.backend.support.MutableClock;

@SpringBootTest
@ActiveProfiles("test")
class LoginAttemptLimiterTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final String KEY = "login-attempt-limiter-test-hmac-key-32bytes";

    @Autowired
    private LoginFailureRepository failures;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        failures.deleteAll();
        clock = new MutableClock(START, ZoneOffset.UTC);
    }

    private LoginAttemptLimiter limiter() {
        return new LoginAttemptLimiter(failures, transactionManager, clock, KEY);
    }

    @Test
    void rejectsShortHmacKey() {
        assertThatThrownBy(() -> new LoginAttemptLimiter(failures, transactionManager, clock, "short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void accountAndIpLimitIsCaseInsensitiveAndExpires() {
        var limiter = limiter();
        for (int i = 0; i < 5; i++)
            limiter.attempt("ip", "User@example.test");
        assertThatThrownBy(() -> limiter.attempt("ip", " user@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
        assertThatCode(() -> limiter.attempt("different-ip", " user@example.test")).doesNotThrowAnyException();
        clock.set(START.plusSeconds(600));
        assertThatCode(() -> limiter.attempt("ip", "User@example.test")).doesNotThrowAnyException();
    }

    @Test
    void accountLimitCoversDistributedAttempts() {
        var limiter = limiter();
        for (int i = 0; i < 50; i++)
            limiter.attempt("ip-" + i, "distributed@example.test");
        assertThatThrownBy(() -> limiter.attempt("new-ip", "distributed@example.test"))
                .isInstanceOf(LoginAttemptLimiter.Limited.class);
    }

    @Test
    void retryAfterIsRemainingTime() {
        var limiter = limiter();
        for (int i = 0; i < 5; i++)
            limiter.attempt("ip", "wait@example.test");
        clock.set(START.plusSeconds(200).plusMillis(500));
        assertThatThrownBy(() -> limiter.attempt("ip", "wait@example.test"))
                .isInstanceOfSatisfying(LoginAttemptLimiter.Limited.class, limited -> assertThat(limited.getRetryAfterSeconds()).isEqualTo(400));
    }

    @Test
    void ipLimitCoversDifferentAccounts() {
        var limiter = limiter();
        for (int i = 0; i < 30; i++)
            limiter.attempt("ip", i + "@example.test");
        assertThatThrownBy(() -> limiter.attempt("ip", "new@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
        assertThatCode(() -> limiter.attempt("other-ip", "new@example.test")).doesNotThrowAnyException();
    }

    @Test
    void successResetsAccountAndGivesBackIpAttempt() {
        var limiter = limiter();
        for (int i = 0; i < 29; i++)
            limiter.attempt("ip", i + "@example.test");
        for (int i = 0; i < 5; i++)
            limiter.attempt("other-ip", "good@example.test");
        // 30번째 시도가 성공하면 주소 횟수는 29로 돌아가고 계정 기록은 지워진다.
        limiter.attempt("ip", "good-ip@example.test");
        limiter.succeeded("ip", "good-ip@example.test");
        limiter.succeeded("other-ip", "good@example.test");
        assertThatCode(() -> limiter.attempt("ip", "good@example.test")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.attempt("ip", "another@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
    }

    @Test
    void limitSurvivesRestart() {
        var beforeRestart = limiter();
        for (int i = 0; i < 5; i++)
            beforeRestart.attempt("ip", "restart@example.test");
        // 서버 재시작: 새 제한기 객체도 같은 DB 기록을 본다.
        var afterRestart = limiter();
        assertThatThrownBy(() -> afterRestart.attempt("ip", "restart@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
    }

    @Test
    void storesOnlyHmacKeys() throws Exception {
        limiter().attempt("203.0.113.7", "private@example.test");
        String plainHash = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest("private@example.test".getBytes(StandardCharsets.UTF_8)));
        assertThat(failures.findAll()).extracting(LoginFailure::getKey)
                .hasSize(3)
                .allSatisfy(key -> assertThat(key).matches("(account|account-ip|ip):[0-9a-f]{64}"))
                .noneMatch(key -> key.contains("private") || key.contains("203.0.113.7") || key.endsWith(plainHash));
    }

    @Test
    void cleanupRemovesExpiredRecords() {
        var limiter = limiter();
        limiter.attempt("ip", "old@example.test");
        clock.set(START.plusSeconds(601));
        limiter.attempt("ip-new", "new@example.test");
        assertThat(limiter.cleanup()).isEqualTo(3);
        assertThat(failures.count()).isEqualTo(3);
    }

    @Test
    void cleanupSkipsRecordChangedAfterRead() {
        var limiter = limiter();
        limiter.attempt("ip", "race@example.test");
        clock.set(START.plusSeconds(601));
        // 정리 작업이 만료 행을 읽은 직후, 같은 계정의 로그인 시도가 그 행을 새 기간으로 갱신한다.
        LoginFailureRepository racing = mock(LoginFailureRepository.class, AdditionalAnswers.delegatesTo(failures));
        doAnswer(invocation -> {
            List<LoginFailure> expired = failures.findByStartedAtLessThanEqual(invocation.getArgument(0));
            limiter.attempt("ip", "race@example.test");
            return expired;
        }).when(racing).findByStartedAtLessThanEqual(any(LocalDateTime.class));
        var cleaner = new LoginAttemptLimiter(racing, transactionManager, clock, KEY);
        assertThat(cleaner.cleanup()).isZero();
        assertThat(failures.findAll()).hasSize(3).allSatisfy(failure -> assertThat(failure.getCount()).isEqualTo(1));
    }

    @Test
    void concurrentAttemptsOnSameAccountAllowExactlyLimit() throws Exception {
        var limiter = limiter();
        assertThat(runConcurrently(limiter, 30, "parallel@example.test")).isEqualTo(5);
    }

    /** 같은 계정으로 동시에 시도하고 비밀번호 검사까지 간 시도 수를 센다. */
    static int runConcurrently(LoginAttemptLimiter limiter, int threads, String email) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            var ready = new CountDownLatch(threads);
            var go = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String ip = "concurrent-ip";
                Callable<Boolean> task = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        limiter.attempt(ip, email);
                        return true;
                    } catch (LoginAttemptLimiter.Limited limited) {
                        return false;
                    }
                };
                results.add(pool.submit(task));
            }
            ready.await();
            go.countDown();
            int proceeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get())
                    proceeded++;
            }
            return proceeded;
        } finally {
            pool.shutdownNow();
        }
    }
}
