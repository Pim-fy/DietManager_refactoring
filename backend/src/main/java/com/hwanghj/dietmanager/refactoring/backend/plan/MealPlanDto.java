package com.hwanghj.dietmanager.refactoring.backend.plan;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import com.hwanghj.dietmanager.refactoring.backend.food.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.intake.IntakeDto;
import com.hwanghj.dietmanager.refactoring.backend.intake.MealType;
public final class MealPlanDto {
    public record Request(@NotBlank @Size(max=100) String foodCode,@NotNull LocalDate plannedDate,@NotNull MealType mealType,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal amount) {}
    public record Response(Long id,LocalDate plannedDate,MealType mealType,BigDecimal amount,FoodNutrition food,IntakeDto.Response nutrition,boolean deleted,Long intakeId) {}
}
