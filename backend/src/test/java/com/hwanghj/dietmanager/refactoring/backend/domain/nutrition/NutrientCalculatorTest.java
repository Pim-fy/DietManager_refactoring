package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service.NutrientCalculator;

class NutrientCalculatorTest {
    @Test void scalesAndRoundsOnlyAtFinalDivision() {
        assertThat(NutrientCalculator.scale(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("8"))).isEqualByComparingTo("0.13");
        assertThat(NutrientCalculator.scale(new BigDecimal("250"), new BigDecimal("200"), new BigDecimal("100"))).isEqualByComparingTo("500");
    }
    @Test void unknownValuesStayUnknownAndAreCounted() {
        assertThat(NutrientCalculator.scale(null, BigDecimal.ONE, BigDecimal.TEN)).isNull();
        var total = NutrientCalculator.sum(Arrays.asList(new BigDecimal("1.25"), null, new BigDecimal("2.50")), value -> value);
        assertThat(total.getValue()).isEqualByComparingTo("3.75");
        assertThat(total.getMissingCount()).isEqualTo(1);
    }
}
