package com.hwanghj.dietmanager.refactoring.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;

class JwtClockTest {
    @Test
    void tokenExpiresUsingInjectedTime() {
        var start = Instant.parse("2026-01-01T00:00:00Z");
        String key = "synthetic-clock-test-key-at-least-thirty-two-bytes";
        var user = User.builder().email("clock@example.test").passwordHash("synthetic").userName("clock").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        String token = new JwtTokenProvider(key, 1000, Clock.fixed(start, ZoneOffset.UTC)).createAccessToken(user);
        assertThat(new JwtTokenProvider(key, 1000, Clock.fixed(start.plusMillis(999), ZoneOffset.UTC)).validateToken(token)).isTrue();
        assertThat(new JwtTokenProvider(key, 1000, Clock.fixed(start.plusSeconds(2), ZoneOffset.UTC)).validateToken(token)).isFalse();
    }
}
