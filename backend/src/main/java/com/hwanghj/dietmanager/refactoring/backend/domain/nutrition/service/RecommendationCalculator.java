package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto.NutritionDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.Gender;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;

public final class RecommendationCalculator {
    private static final String FORMULA = "Mifflin–St Jeor";

    private RecommendationCalculator() {
    }

    public static NutritionDto.Recommendation calculate(double weight, double height, int age, Gender gender, GoalType goal, ActivityLevel activity,
            LocalDate date, LocalDate measurementDate) {
        double resting = 10 * weight + 6.25 * height - 5 * age + (gender == Gender.MALE ? 5 : -161);
        double maintenance = resting * activity.factor;
        double target = maintenance + switch (goal) {
        case LOSE_WEIGHT -> -300;
        case GAIN_WEIGHT -> 300;
        case MAINTAIN_WEIGHT -> 0;
        };
        if (!Double.isFinite(target) || resting <= 0 || target <= 0)
            return unavailable(date, activity, "신체 기록을 확인해주세요. 현재 값으로 권장량을 계산할 수 없습니다.");
        return NutritionDto.Recommendation.builder()
                .available(true)
                .formula(FORMULA)
                .date(date)
                .measurementDate(measurementDate)
                .activityLevel(activity)
                .restingCalories(rounded(resting))
                .maintenanceCalories(rounded(maintenance))
                .calories(rounded(target))
                .carbohydrate(rounded(target * .5 / 4))
                .protein(rounded(target * .2 / 4))
                .fat(rounded(target * .3 / 9))
                .build();
    }

    public static NutritionDto.Recommendation unavailable(LocalDate date, ActivityLevel activity, String reason) {
        return NutritionDto.Recommendation.builder()
                .available(false)
                .reason(reason)
                .formula(FORMULA)
                .date(date)
                .activityLevel(activity)
                .build();
    }

    private static BigDecimal rounded(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
