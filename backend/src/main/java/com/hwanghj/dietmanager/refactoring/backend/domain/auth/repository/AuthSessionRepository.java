package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    List<AuthSession> findTop500ByExpiresAtLessThanEqualOrRevokedTrueOrderById(LocalDateTime now);
}
