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

        @NotBlank
        @Size(max = 100)
        private String foodCode;

        @NotNull
        private LocalDate plannedDate;

        @NotNull
        private MealType mealType;

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 2)
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
