package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.AuthSession;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> locked(String hash);

    @Modifying
    @Query("delete from RefreshToken t where t.session.id in (select s.id from AuthSession s where s.expiresAt <= :now or s.revoked = true)")
    int deleteInactive(LocalDateTime now);
}
