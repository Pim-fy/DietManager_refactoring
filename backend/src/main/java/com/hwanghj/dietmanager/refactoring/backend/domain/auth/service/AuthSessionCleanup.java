package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.AuthSession;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.RefreshToken;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.AuthSessionRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.RefreshTokenRepository;

@Service
@EnableScheduling
public class AuthSessionCleanup {
    private final Clock clock;
    private final RefreshTokenRepository tokens;
    private final AuthSessionRepository sessions;

    private final TransactionTemplate batchTransaction;

    public AuthSessionCleanup(RefreshTokenRepository tokens, AuthSessionRepository sessions, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.tokens = tokens;
        this.sessions = sessions;
        this.batchTransaction = new TransactionTemplate(transactionManager);
        this.batchTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        while (Boolean.TRUE.equals(batchTransaction.execute(status -> deleteBatch(now)))) {
            // 이번 묶음을 커밋한 뒤 다음 대상을 조회한다.
        }
    }

    private boolean deleteBatch(LocalDateTime now) {
        List<AuthSession> expired = sessions.findTop500ByExpiresAtLessThanEqualOrRevokedTrueOrderById(now);
        if (expired.isEmpty())
            return false;

        List<String> sessionIds = expired.stream().map(AuthSession::getId).toList();
        List<RefreshToken> related = tokens.findTop500BySessionIdInOrderByTokenHash(sessionIds);
        // 한 세션에 토큰이 500개를 넘을 수 있으므로, 토큰을 모두 지운 뒤에 세션을 지운다.
        if (related.isEmpty())
            sessions.deleteAllInBatch(expired);
        else
            tokens.deleteAllInBatch(related);
        return true;
    }
}
