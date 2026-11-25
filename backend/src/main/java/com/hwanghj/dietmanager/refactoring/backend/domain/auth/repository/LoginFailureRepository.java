package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.LoginFailure;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface LoginFailureRepository extends JpaRepository<LoginFailure, String> {

    // 같은 키의 동시 시도를 줄 세운다. 공격으로 몰려도 스레드가 오래 묶이지 않게 3초까지만 기다린다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"),
            @QueryHint(name = "jakarta.persistence.query.timeout", value = "3000") })
    Optional<LoginFailure> findWithLockByKey(String key);

    // 정리 대상(제한 시간이 지난 기록)
    List<LoginFailure> findByStartedAtLessThanEqual(LocalDateTime windowStart);
}
