package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession,String> {
    @Modifying
    @Query("delete from AuthSession s where s.expiresAt <= :now or s.revoked = true")
    int deleteInactive(LocalDateTime now);
}
