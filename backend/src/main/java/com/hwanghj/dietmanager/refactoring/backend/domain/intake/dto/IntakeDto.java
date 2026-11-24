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

        @NotBlank(message = "음식명은 필수입니다.")
        @Size(max = 500, message = "음식명은 500자 이하여야 합니다.")
        private String foodName;

        @NotNull(message = "기준량은 필수입니다.")
        @DecimalMin(value = "0.01", message = "기준량은 0.01 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "기준량은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 2, message = "기준량은 소수점 2자리까지 입력할 수 있습니다.")
        private BigDecimal baseAmount;

        @DecimalMin(value = "0", message = "칼로리는 0 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "칼로리는 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 6, message = "칼로리는 소수점 6자리까지 입력할 수 있습니다.")
        private BigDecimal calories;

        @DecimalMin(value = "0", message = "탄수화물은 0 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "탄수화물은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 6, message = "탄수화물은 소수점 6자리까지 입력할 수 있습니다.")
        private BigDecimal carbohydrate;

        @DecimalMin(value = "0", message = "단백질은 0 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "단백질은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 6, message = "단백질은 소수점 6자리까지 입력할 수 있습니다.")
        private BigDecimal protein;

        @DecimalMin(value = "0", message = "지방은 0 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "지방은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 6, message = "지방은 소수점 6자리까지 입력할 수 있습니다.")
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

        @NotNull(message = "영양정보는 필수입니다.")
        @Valid
        private NutritionInput nutrition;

        @NotBlank(message = "단위는 필수입니다.")
        @Pattern(regexp = "g|ml", message = "단위는 g 또는 ml이어야 합니다.")
        private String unit;

        @NotNull(message = "섭취일은 필수입니다.")
        @PastOrPresent(message = "섭취일은 오늘 또는 과거 날짜여야 합니다.")
        private LocalDate eatenDate;

        @NotNull(message = "끼니는 필수입니다.")
        private MealType mealType;

        @NotNull(message = "섭취량은 필수입니다.")
        @DecimalMin(value = "0.01", message = "섭취량은 0.01 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "섭취량은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 2, message = "섭취량은 소수점 2자리까지 입력할 수 있습니다.")
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

        @NotBlank(message = "식품 코드는 필수입니다.")
        @Size(max = 100, message = "식품 코드는 100자 이하여야 합니다.")
        private String foodCode;

        @NotNull(message = "섭취일은 필수입니다.")
        @PastOrPresent(message = "섭취일은 오늘 또는 과거 날짜여야 합니다.")
        private LocalDate eatenDate;

        @NotNull(message = "끼니는 필수입니다.")
        private MealType mealType;

        @NotNull(message = "섭취량은 필수입니다.")
        @DecimalMin(value = "0.01", message = "섭취량은 0.01 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "섭취량은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 2, message = "섭취량은 소수점 2자리까지 입력할 수 있습니다.")
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

        @NotNull(message = "섭취일은 필수입니다.")
        @PastOrPresent(message = "섭취일은 오늘 또는 과거 날짜여야 합니다.")
        private LocalDate eatenDate;

        @NotNull(message = "끼니는 필수입니다.")
        private MealType mealType;

        @NotNull(message = "섭취량은 필수입니다.")
        @DecimalMin(value = "0.01", message = "섭취량은 0.01 이상이어야 합니다.")
        @DecimalMax(value = "1000000", message = "섭취량은 1,000,000 이하여야 합니다.")
        @Digits(integer = 7, fraction = 2, message = "섭취량은 소수점 2자리까지 입력할 수 있습니다.")
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
