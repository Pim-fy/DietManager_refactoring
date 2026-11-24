package com.hwanghj.dietmanager.refactoring.backend.nutrition;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.hwanghj.dietmanager.refactoring.backend.common.*;
import com.hwanghj.dietmanager.refactoring.backend.global.common.*;
import static org.assertj.core.api.Assertions.assertThat;
class RecommendationCalculatorTest {
    @Test void keepsFormulaAndMacroRatios() {
        var date = LocalDate.of(2026, 1, 1);
        var result = RecommendationCalculator.calculate(70, 170, 30, Gender.MALE, GoalType.MAINTAIN_WEIGHT, ActivityLevel.values()[0], date, date);
        assertThat(result.restingCalories()).isEqualByComparingTo("1617.5");
        assertThat(result.calories()).isEqualByComparingTo("1941");
        assertThat(result.carbohydrate()).isEqualByComparingTo("242.63");
        assertThat(result.protein()).isEqualByComparingTo("97.05");
        assertThat(result.fat()).isEqualByComparingTo("64.7");
    }
    @Test void invalidCalculationReturnsUnavailable() {
        var date = LocalDate.of(2026, 1, 1);
        assertThat(RecommendationCalculator.calculate(Double.NaN, 170, 30, Gender.FEMALE, GoalType.LOSE_WEIGHT, ActivityLevel.values()[0], date, date).available()).isFalse();
    }
}
