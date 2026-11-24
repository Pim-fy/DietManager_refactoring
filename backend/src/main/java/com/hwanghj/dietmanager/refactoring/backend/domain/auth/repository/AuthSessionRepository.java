package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    // 만료됐거나 폐기된 세션 삭제. 반환값은 삭제한 세션 수.
    long deleteByExpiresAtLessThanEqualOrRevokedTrue(LocalDateTime now);
}
