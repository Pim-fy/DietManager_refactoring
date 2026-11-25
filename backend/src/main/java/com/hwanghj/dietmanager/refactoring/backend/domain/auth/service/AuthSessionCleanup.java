package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.AuthSessionRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.RefreshTokenRepository;

@Service
@EnableScheduling
public class AuthSessionCleanup {
    private final Clock clock;
    private final RefreshTokenRepository tokens;
    private final AuthSessionRepository sessions;

    public AuthSessionCleanup(RefreshTokenRepository tokens, AuthSessionRepository sessions) {
        this(tokens, sessions, Clock.systemDefaultZone());
    }

    @Autowired
    public AuthSessionCleanup(RefreshTokenRepository tokens, AuthSessionRepository sessions, Clock clock) {
        this.clock = clock;
        this.tokens = tokens;
        this.sessions = sessions;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    @Transactional
    public void cleanup() {
        var now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        tokens.deleteBySessionExpiresAtLessThanEqualOrSessionRevokedTrue(now);
        sessions.deleteByExpiresAtLessThanEqualOrRevokedTrue(now);
    }
}
