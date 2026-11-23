package com.hwanghj.dietmanager.refactoring.backend.nutrition;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class NutritionDto {
    public record Preference(ActivityLevel activityLevel) {}
    public record Recommendation(boolean available, String reason, String formula, LocalDate date, LocalDate measurementDate,
        ActivityLevel activityLevel, BigDecimal restingCalories, BigDecimal maintenanceCalories, BigDecimal calories,
        BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {}
}
