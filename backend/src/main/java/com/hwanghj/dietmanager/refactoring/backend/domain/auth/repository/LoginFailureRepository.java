package com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.LoginFailure;

public interface LoginFailureRepository extends JpaRepository<LoginFailure, String> {

    // 제한 시간이 지난 기록 정리
    @Modifying
    @Query("delete from LoginFailure f where f.startedAt <= :windowStart")
    int deleteExpired(@Param("windowStart") LocalDateTime windowStart);
}
