package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.LoginFailure;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.LoginFailureRepository;

@SpringBootTest
@ActiveProfiles("test")
class LoginAttemptLimiterTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private LoginFailureRepository failures;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Clock clock;

    @BeforeEach
    void setUp() {
        failures.deleteAll();
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(START);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.withZone(ZoneOffset.UTC)).thenReturn(clock);
    }

    private LoginAttemptLimiter limiter() {
        return new LoginAttemptLimiter(failures, transactionManager, clock);
    }

    @Test
    void accountLimitIsCaseInsensitiveAndExpires() {
        var limiter = limiter();
        for (int i = 0; i < 5; i++) {
            limiter.check("ip", "User@example.test");
            limiter.failed("ip", "User@example.test");
        }
        assertThatThrownBy(() -> limiter.check("different-ip", "user@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
        when(clock.instant()).thenReturn(START.plusSeconds(600));
        assertThatCode(() -> limiter.check("ip", "User@example.test")).doesNotThrowAnyException();
    }

    @Test
    void ipLimitCoversDifferentAccountsAndSuccessfulLoginResetsAccount() {
        var limiter = limiter();
        for (int i = 0; i < 30; i++)
            limiter.failed("ip", i + "@example.test");
        assertThatThrownBy(() -> limiter.check("ip", "new@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
        for (int i = 0; i < 5; i++)
            limiter.failed("other-ip", "good@example.test");
        limiter.succeeded("good@example.test");
        assertThatCode(() -> limiter.check("other-ip", "good@example.test")).doesNotThrowAnyException();
    }

    @Test
    void limitSurvivesRestart() {
        var beforeRestart = limiter();
        for (int i = 0; i < 5; i++)
            beforeRestart.failed("ip", "restart@example.test");
        // 서버 재시작: 새 제한기 객체도 같은 DB 기록을 본다.
        var afterRestart = limiter();
        assertThatThrownBy(() -> afterRestart.check("ip", "restart@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
    }

    @Test
    void storesOnlyHashedKeys() {
        limiter().failed("203.0.113.7", "private@example.test");
        assertThat(failures.findAll()).extracting(LoginFailure::getKey)
                .hasSize(2)
                .allSatisfy(key -> assertThat(key).matches("(account|ip):[0-9a-f]{64}"))
                .noneMatch(key -> key.contains("private") || key.contains("203.0.113.7"));
    }

    @Test
    void cleanupRemovesExpiredRecords() {
        var limiter = limiter();
        limiter.failed("ip", "old@example.test");
        when(clock.instant()).thenReturn(START.plusSeconds(601));
        limiter.failed("ip-new", "new@example.test");
        limiter.cleanup();
        assertThat(failures.count()).isEqualTo(2);
    }
}
