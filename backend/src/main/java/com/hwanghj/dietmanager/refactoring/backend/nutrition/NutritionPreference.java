package com.hwanghj.dietmanager.refactoring.backend.nutrition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity @Table(name = "nutrition_preferences") @Getter @NoArgsConstructor
public class NutritionPreference {
    @Id private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ActivityLevel activityLevel;
    public NutritionPreference(Long userId, ActivityLevel activity) { this.userId = userId; this.activityLevel = activity; }
    public void update(ActivityLevel activity) { this.activityLevel = activity; }
}
