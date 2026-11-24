package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.Gender;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.GoalType;
public final class RecommendationCalculator {
    private RecommendationCalculator() {}
    public static NutritionDto.Recommendation calculate(double weight, double height, int age, Gender gender, GoalType goal, ActivityLevel activity, LocalDate date, LocalDate measurementDate) {
        double resting = 10 * weight + 6.25 * height - 5 * age + (gender == Gender.MALE ? 5 : -161);
        double maintenance = resting * activity.factor;
        double target = maintenance + switch (goal) { case LOSE_WEIGHT -> -300; case GAIN_WEIGHT -> 300; case MAINTAIN_WEIGHT -> 0; };
        if (!Double.isFinite(target) || resting <= 0 || target <= 0) return unavailable(date, activity, "신체 기록을 확인해주세요. 현재 값으로 권장량을 계산할 수 없습니다.");
        return new NutritionDto.Recommendation(true, null, "Mifflin–St Jeor", date, measurementDate, activity,
            rounded(resting), rounded(maintenance), rounded(target), rounded(target * .5 / 4), rounded(target * .2 / 4), rounded(target * .3 / 9));
    }
    public static NutritionDto.Recommendation unavailable(LocalDate date, ActivityLevel activity, String reason) {
        return new NutritionDto.Recommendation(false, reason, "Mifflin–St Jeor", date, null, activity, null, null, null, null, null, null);
    }
    private static BigDecimal rounded(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }
}
