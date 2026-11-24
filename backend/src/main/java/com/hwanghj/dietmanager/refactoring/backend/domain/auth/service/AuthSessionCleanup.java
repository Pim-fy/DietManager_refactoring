package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.AuthSessionRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.RefreshTokenRepository;
@Service @EnableScheduling
public class AuthSessionCleanup {
    private final java.time.Clock clock;
    private final RefreshTokenRepository tokens;
    private final AuthSessionRepository sessions;
    public AuthSessionCleanup(RefreshTokenRepository tokens,AuthSessionRepository sessions) { this(tokens, sessions, java.time.Clock.systemDefaultZone()); }
    @org.springframework.beans.factory.annotation.Autowired
    public AuthSessionCleanup(RefreshTokenRepository tokens,AuthSessionRepository sessions, java.time.Clock clock) { this.clock = clock; this.tokens=tokens; this.sessions=sessions;  }
    @Scheduled(fixedDelay=3600000,initialDelay=3600000) @Transactional
    public void cleanup() {
        var now=java.time.LocalDateTime.now(clock.withZone(java.time.ZoneOffset.UTC));
        tokens.deleteInactive(now); sessions.deleteInactive(now);
    }
}
