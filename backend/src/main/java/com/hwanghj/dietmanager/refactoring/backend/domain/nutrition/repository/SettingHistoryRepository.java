package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;

public interface SettingHistoryRepository extends JpaRepository<SettingHistory, Long> {

    // 조회 날짜에 적용 중인 설정: 그 날짜 이전 이력 중 가장 최근
    Optional<SettingHistory> findTopByUserIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(Long userId, LocalDate date);

    // 가장 오래된 이력. 조회 날짜가 모든 이력보다 이전일 때 쓴다.
    Optional<SettingHistory> findTopByUserIdOrderByEffectiveDateAsc(Long userId);

    Optional<SettingHistory> findByUserIdAndEffectiveDate(Long userId, LocalDate date);

    boolean existsByUserId(Long userId);
}
