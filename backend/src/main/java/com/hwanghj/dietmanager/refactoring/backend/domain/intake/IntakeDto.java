package com.hwanghj.dietmanager.refactoring.backend.domain.intake;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.*;

public class IntakeDto {
    public record NutritionInput(@NotBlank @Size(max=500) String foodName,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal baseAmount,
        @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=6) BigDecimal calories,
        @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=6) BigDecimal carbohydrate,
        @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=6) BigDecimal protein,
        @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=6) BigDecimal fat) {}
    public record ManualRequest(@NotNull @jakarta.validation.Valid NutritionInput nutrition,
        @NotBlank @Pattern(regexp="g|ml") String unit,
        @NotNull @PastOrPresent LocalDate eatenDate, @NotNull MealType mealType,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal amount) {}
    public record CreateRequest(@NotBlank @Size(max = 100) String foodCode,
        @NotNull @PastOrPresent LocalDate eatenDate, @NotNull MealType mealType,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal amount) {}
    public record UpdateRequest(@NotNull @PastOrPresent LocalDate eatenDate, @NotNull MealType mealType,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal amount) {}
    public record Response(Long id, String foodCode, String foodName, String makerName, LocalDate eatenDate, MealType mealType,
        BigDecimal amount, String unit, BigDecimal baseAmount, BigDecimal calories, BigDecimal carbohydrate,
        BigDecimal protein, BigDecimal fat, String sourceUpdatedDate,
        BigDecimal baseCalories, BigDecimal baseCarbohydrate, BigDecimal baseProtein, BigDecimal baseFat,
        boolean manual, boolean nutritionEdited) {}
    public record Total(BigDecimal value, int missingCount) {}
    public record DailyResponse(LocalDate date, List<Response> items, List<Response> deletedItems, Total calories, Total carbohydrate, Total protein, Total fat) {}
}
