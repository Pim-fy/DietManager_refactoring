package com.hwanghj.dietmanager.refactoring.backend.domain.plan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.dto.IntakeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.MealType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class MealPlanDto {

    /**
     * 식단 계획 생성·수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Request {

        @NotBlank(message = "식품 코드는 필수입니다.")
        @Size(max = 100, message = "식품 코드는 100자 이하여야 합니다.")
        private String foodCode;

        @NotNull(message = "계획일은 필수입니다.")
        private LocalDate plannedDate;

        @NotNull(message = "끼니는 필수입니다.")
        private MealType mealType;

        @NotNull(message = "계획량은 필수입니다.")
        @DecimalMin(value = "0.01", message = "계획량은 0.01 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "계획량은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 2, message = "계획량은 소수점 2자리까지 입력할 수 있습니다.")
        private BigDecimal amount;
    }

    /**
     * 식단 계획 응답 DTO. nutrition은 계획한 양 기준의 영양값 미리보기.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {
        private Long id;
        private LocalDate plannedDate;
        private MealType mealType;
        private BigDecimal amount;
        private FoodNutrition food;
        private IntakeDto.Response nutrition;
        private boolean deleted;
        private Long intakeId;
    }
}
