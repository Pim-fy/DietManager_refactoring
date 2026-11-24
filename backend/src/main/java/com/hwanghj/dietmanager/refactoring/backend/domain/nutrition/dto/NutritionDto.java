package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class NutritionDto {

    /**
     * 활동량 저장 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PreferenceRequest {

        @NotNull(message = "활동량은 필수입니다.")
        private ActivityLevel activityLevel;
    }

    /**
     * 활동량 조회 응답 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Preference {
        private ActivityLevel activityLevel;
    }

    /**
     * 권장 섭취량 응답 DTO. available=false이면 reason에 계산할 수 없는 이유가 있다.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Recommendation {
        private boolean available;
        private String reason;
        private String formula;
        private LocalDate date;
        private LocalDate measurementDate;
        private ActivityLevel activityLevel;
        private BigDecimal restingCalories;
        private BigDecimal maintenanceCalories;
        private BigDecimal calories;
        private BigDecimal carbohydrate;
        private BigDecimal protein;
        private BigDecimal fat;
    }
}
