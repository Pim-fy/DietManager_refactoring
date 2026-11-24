package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
public final class NutrientCalculator {
    public record Total(BigDecimal value, int missingCount) {}
    private NutrientCalculator() {}
    public static BigDecimal scale(BigDecimal nutrient, BigDecimal amount, BigDecimal basis) {
        return nutrient == null ? null : nutrient.multiply(amount).divide(basis, 2, RoundingMode.HALF_UP);
    }
    public static <T> Total sum(List<T> items, Function<T, BigDecimal> nutrient) {
        BigDecimal total = BigDecimal.ZERO;
        int missing = 0;
        for (T item : items) {
            BigDecimal value = nutrient.apply(item);
            if (value == null) missing++; else total = total.add(value);
        }
        return new Total(total, missing);
    }
}
