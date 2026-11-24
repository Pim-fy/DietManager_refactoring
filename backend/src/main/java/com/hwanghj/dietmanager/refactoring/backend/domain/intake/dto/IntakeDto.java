package com.hwanghj.dietmanager.refactoring.backend.domain.intake.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.MealType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class IntakeDto {

    /**
     * 음식명·기준량·기준 영양값 입력 DTO (직접 입력, 영양정보 수정)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class NutritionInput {

        @NotBlank
        @Size(max = 500)
        private String foodName;

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 2)
        private BigDecimal baseAmount;

        @DecimalMin("0")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 6)
        private BigDecimal calories;

        @DecimalMin("0")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 6)
        private BigDecimal carbohydrate;

        @DecimalMin("0")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 6)
        private BigDecimal protein;

        @DecimalMin("0")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 6)
        private BigDecimal fat;
    }

    /**
     * 음식 직접 입력 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ManualRequest {

        @NotNull
        @Valid
        private NutritionInput nutrition;

        @NotBlank
        @Pattern(regexp = "g|ml")
        private String unit;

        @NotNull
        @PastOrPresent
        private LocalDate eatenDate;

        @NotNull
        private MealType mealType;

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 2)
        private BigDecimal amount;
    }

    /**
     * 식품 API 식품으로 섭취 기록 생성 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CreateRequest {

        @NotBlank
        @Size(max = 100)
        private String foodCode;

        @NotNull
        @PastOrPresent
        private LocalDate eatenDate;

        @NotNull
        private MealType mealType;

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 2)
        private BigDecimal amount;
    }

    /**
     * 섭취 기록 날짜·끼니·섭취량 수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UpdateRequest {

        @NotNull
        @PastOrPresent
        private LocalDate eatenDate;

        @NotNull
        private MealType mealType;

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1000000")
        @Digits(integer = 7, fraction = 2)
        private BigDecimal amount;
    }

    /**
     * 섭취 기록 응답 DTO. base* 값은 환산 전 기준 영양값.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {
        private Long id;
        private String foodCode;
        private String foodName;
        private String makerName;
        private LocalDate eatenDate;
        private MealType mealType;
        private BigDecimal amount;
        private String unit;
        private BigDecimal baseAmount;
        private BigDecimal calories;
        private BigDecimal carbohydrate;
        private BigDecimal protein;
        private BigDecimal fat;
        private String sourceUpdatedDate;
        private BigDecimal baseCalories;
        private BigDecimal baseCarbohydrate;
        private BigDecimal baseProtein;
        private BigDecimal baseFat;
        private boolean manual;
        private boolean nutritionEdited;
    }

    /**
     * 영양값 합계. missingCount는 값이 없는 기록 수.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Total {
        private BigDecimal value;
        private int missingCount;
    }

    /**
     * 날짜별 섭취 기록과 합계 응답 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DailyResponse {
        private LocalDate date;
        private List<Response> items;
        private List<Response> deletedItems;
        private Total calories;
        private Total carbohydrate;
        private Total protein;
        private Total fat;
    }
}
