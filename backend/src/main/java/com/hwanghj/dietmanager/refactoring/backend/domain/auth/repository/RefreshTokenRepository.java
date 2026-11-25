package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // 갱신·로그아웃 시 같은 토큰을 동시에 쓰지 못하게 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findWithLockByTokenHash(String tokenHash);

    List<RefreshToken> findTop500BySessionIdInOrderByTokenHash(Collection<String> sessionIds);
}
