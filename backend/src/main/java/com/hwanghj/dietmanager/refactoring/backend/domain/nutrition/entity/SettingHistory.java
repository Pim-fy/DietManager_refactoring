package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity;

import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 목표·활동량 설정 이력.
 * effectiveDate부터 다음 이력 전날까지 이 설정이 적용된 것으로 본다. 사용자·날짜마다 한 행.
 */
@Entity
@Table(name = "setting_histories", uniqueConstraints = @UniqueConstraint(name = "uq_setting_user_date", columnNames = { "user_id", "effective_date" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private GoalType goalType;

    // 활동량을 아직 고르지 않은 기간은 null
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 20)
    private ActivityLevel activityLevel;

    public SettingHistory(Long userId, LocalDate effectiveDate, GoalType goalType, ActivityLevel activityLevel) {
        this.userId = userId;
        this.effectiveDate = effectiveDate;
        this.goalType = goalType;
        this.activityLevel = activityLevel;
    }

    public void update(GoalType goalType, ActivityLevel activityLevel) {
        this.goalType = goalType;
        this.activityLevel = activityLevel;
    }
}
