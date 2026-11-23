package com.hwanghj.dietmanager.refactoring.backend.food;

import java.math.BigDecimal;
import java.util.List;

/** 외부 DB의 기준량과 영양값. 누락 영양값은 null로 유지한다. */
public record FoodNutrition(String foodCode, String foodName, String makerName,
        String servingSize, BigDecimal baseAmount, String baseUnit,
        BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat,
        String sourceUpdatedDate) {
    public boolean recordable() { return baseAmount != null && baseAmount.signum() > 0 && baseUnit != null; }
    public record SearchResult(List<FoodNutrition> items, int page, int size, int totalCount) {}
}
