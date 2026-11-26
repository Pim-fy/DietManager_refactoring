package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;

public interface SettingHistoryRepository extends JpaRepository<SettingHistory, Long> {

    // 사용자의 전체 설정 이력(적용일 오름차순). 날짜별 적용 설정은 RecommendationTimeline이 메모리에서 고른다.
    List<SettingHistory> findByUserIdOrderByEffectiveDateAsc(Long userId);

    Optional<SettingHistory> findByUserIdAndEffectiveDate(Long userId, LocalDate date);

    boolean existsByUserId(Long userId);
}
