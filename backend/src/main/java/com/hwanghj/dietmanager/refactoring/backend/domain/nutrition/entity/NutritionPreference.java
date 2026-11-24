package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "nutrition_preferences") @Getter @NoArgsConstructor
public class NutritionPreference {
    @Id private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ActivityLevel activityLevel;
    public NutritionPreference(Long userId, ActivityLevel activity) { this.userId = userId; this.activityLevel = activity; }
    public void update(ActivityLevel activity) { this.activityLevel = activity; }
}
