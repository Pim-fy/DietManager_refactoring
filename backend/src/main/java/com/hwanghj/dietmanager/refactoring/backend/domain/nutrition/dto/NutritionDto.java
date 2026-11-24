package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
public final class NutritionDto {
    public record Preference(ActivityLevel activityLevel) {}
    public record Recommendation(boolean available, String reason, String formula, LocalDate date, LocalDate measurementDate,
        ActivityLevel activityLevel, BigDecimal restingCalories, BigDecimal maintenanceCalories, BigDecimal calories,
        BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {}
}
