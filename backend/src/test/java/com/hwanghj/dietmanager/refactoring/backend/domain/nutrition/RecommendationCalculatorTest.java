package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.controller.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.*;
import com.hwanghj.dietmanager.refactoring.backend.global.common.*;
import static org.assertj.core.api.Assertions.assertThat;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service.RecommendationCalculator;
class RecommendationCalculatorTest {
    @Test void keepsFormulaAndMacroRatios() {
        var date = LocalDate.of(2026, 1, 1);
        var result = RecommendationCalculator.calculate(70, 170, 30, Gender.MALE, GoalType.MAINTAIN_WEIGHT, ActivityLevel.values()[0], date, date);
        assertThat(result.getRestingCalories()).isEqualByComparingTo("1617.5");
        assertThat(result.getCalories()).isEqualByComparingTo("1941");
        assertThat(result.getCarbohydrate()).isEqualByComparingTo("242.63");
        assertThat(result.getProtein()).isEqualByComparingTo("97.05");
        assertThat(result.getFat()).isEqualByComparingTo("64.7");
    }
    @Test void invalidCalculationReturnsUnavailable() {
        var date = LocalDate.of(2026, 1, 1);
        assertThat(RecommendationCalculator.calculate(Double.NaN, 170, 30, Gender.FEMALE, GoalType.LOSE_WEIGHT, ActivityLevel.values()[0], date, date).isAvailable()).isFalse();
    }
}
