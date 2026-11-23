package com.hwanghj.dietmanager.refactoring.backend.intake;
import java.math.BigDecimal;
import com.hwanghj.dietmanager.refactoring.backend.nutrition.NutrientCalculator;
public final class IntakeMapper {
    private IntakeMapper() {}
    private static BigDecimal scale(BigDecimal nutrient, IntakeRecord record) {
        return NutrientCalculator.scale(nutrient, record.getAmount(), record.effectiveBaseAmount());
    }
    public static IntakeDto.Response response(IntakeRecord record) {
        return new IntakeDto.Response(record.getId(), record.getFoodCode(), record.effectiveFoodName(), record.getMakerName(), record.getEatenDate(), record.getMealType(),
            record.getAmount(), record.getUnit(), record.effectiveBaseAmount(), scale(record.effectiveCalories(), record), scale(record.effectiveCarbohydrate(), record),
            scale(record.effectiveProtein(), record), scale(record.effectiveFat(), record), record.getSourceUpdatedDate(),
            record.effectiveCalories(),record.effectiveCarbohydrate(),record.effectiveProtein(),record.effectiveFat(),record.isManual(),record.isNutritionEdited());
    }
    public static IntakeDto.Response preview(com.hwanghj.dietmanager.refactoring.backend.food.FoodNutrition food, java.time.LocalDate date, MealType meal, BigDecimal amount) {
        return new IntakeDto.Response(null, food.foodCode(), food.foodName(), food.makerName(), date, meal, amount,
            food.baseUnit(), food.baseAmount(), NutrientCalculator.scale(food.calories(), amount, food.baseAmount()),
            NutrientCalculator.scale(food.carbohydrate(), amount, food.baseAmount()), NutrientCalculator.scale(food.protein(), amount, food.baseAmount()),
            NutrientCalculator.scale(food.fat(), amount, food.baseAmount()), food.sourceUpdatedDate(), food.calories(), food.carbohydrate(), food.protein(), food.fat(), false, false);
    }
    public static IntakeDto.Total total(java.util.List<IntakeDto.Response> items,
            java.util.function.Function<IntakeDto.Response, BigDecimal> nutrient) {
        var total = NutrientCalculator.sum(items, nutrient);
        return new IntakeDto.Total(total.value(), total.missingCount());
    }
    public static IntakeDto.Total aggregate(java.util.List<IntakeRecord> records,
            java.util.function.Function<IntakeRecord, BigDecimal> nutrient) {
        var total = NutrientCalculator.sum(records, record ->
            NutrientCalculator.scale(nutrient.apply(record), record.getAmount(), record.effectiveBaseAmount()));
        return new IntakeDto.Total(total.value(), total.missingCount());
    }
}
