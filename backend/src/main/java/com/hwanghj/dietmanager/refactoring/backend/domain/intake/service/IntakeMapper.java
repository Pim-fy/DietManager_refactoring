package com.hwanghj.dietmanager.refactoring.backend.domain.intake.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.dto.IntakeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.IntakeRecord;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.MealType;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service.NutrientCalculator;

public final class IntakeMapper {
    private IntakeMapper() {
    }

    private static BigDecimal scale(BigDecimal nutrient, IntakeRecord record) {
        return NutrientCalculator.scale(nutrient, record.getAmount(), record.effectiveBaseAmount());
    }

    public static IntakeDto.Response response(IntakeRecord record) {
        return IntakeDto.Response.builder()
                .id(record.getId())
                .foodCode(record.getFoodCode())
                .foodName(record.effectiveFoodName())
                .makerName(record.getMakerName())
                .eatenDate(record.getEatenDate())
                .mealType(record.getMealType())
                .amount(record.getAmount())
                .unit(record.getUnit())
                .baseAmount(record.effectiveBaseAmount())
                .calories(scale(record.effectiveCalories(), record))
                .carbohydrate(scale(record.effectiveCarbohydrate(), record))
                .protein(scale(record.effectiveProtein(), record))
                .fat(scale(record.effectiveFat(), record))
                .sourceUpdatedDate(record.getSourceUpdatedDate())
                .baseCalories(record.effectiveCalories())
                .baseCarbohydrate(record.effectiveCarbohydrate())
                .baseProtein(record.effectiveProtein())
                .baseFat(record.effectiveFat())
                .manual(record.isManual())
                .nutritionEdited(record.isNutritionEdited())
                .build();
    }

    public static IntakeDto.Response preview(FoodNutrition food, LocalDate date, MealType meal, BigDecimal amount) {
        BigDecimal basis = food.getBaseAmount();
        return IntakeDto.Response.builder()
                .foodCode(food.getFoodCode())
                .foodName(food.getFoodName())
                .makerName(food.getMakerName())
                .eatenDate(date)
                .mealType(meal)
                .amount(amount)
                .unit(food.getBaseUnit())
                .baseAmount(basis)
                .calories(NutrientCalculator.scale(food.getCalories(), amount, basis))
                .carbohydrate(NutrientCalculator.scale(food.getCarbohydrate(), amount, basis))
                .protein(NutrientCalculator.scale(food.getProtein(), amount, basis))
                .fat(NutrientCalculator.scale(food.getFat(), amount, basis))
                .sourceUpdatedDate(food.getSourceUpdatedDate())
                .baseCalories(food.getCalories())
                .baseCarbohydrate(food.getCarbohydrate())
                .baseProtein(food.getProtein())
                .baseFat(food.getFat())
                .manual(false)
                .nutritionEdited(false)
                .build();
    }

    public static IntakeDto.Total total(List<IntakeDto.Response> items, Function<IntakeDto.Response, BigDecimal> nutrient) {
        return toTotal(NutrientCalculator.sum(items, nutrient));
    }

    public static IntakeDto.Total aggregate(List<IntakeRecord> records, Function<IntakeRecord, BigDecimal> nutrient) {
        var total = NutrientCalculator.sum(records,
                record -> NutrientCalculator.scale(nutrient.apply(record), record.getAmount(), record.effectiveBaseAmount()));
        return toTotal(total);
    }

    private static IntakeDto.Total toTotal(NutrientCalculator.Total total) {
        return IntakeDto.Total.builder()
                .value(total.getValue())
                .missingCount(total.getMissingCount())
                .build();
    }
}
