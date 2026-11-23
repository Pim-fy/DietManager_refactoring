package com.hwanghj.dietmanager.refactoring.backend.nutrition;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hwanghj.dietmanager.refactoring.backend.profile.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.measurement.BodyMeasurementRepository;

@Service
public class NutritionService {
    private final NutritionPreferenceRepository preferences;
    private final UserProfileRepository profiles;
    private final BodyMeasurementRepository measurements;
    public NutritionService(NutritionPreferenceRepository preferences, UserProfileRepository profiles, BodyMeasurementRepository measurements) {
        this.preferences = preferences; this.profiles = profiles; this.measurements = measurements;
    }
    @Transactional(readOnly = true)
    public NutritionDto.Preference preference(Long userId) { return new NutritionDto.Preference(preferences.findById(userId).map(NutritionPreference::getActivityLevel).orElse(null)); }
    @Transactional
    public NutritionDto.Preference save(Long userId, ActivityLevel activity) {
        var preference = preferences.findById(userId).orElseGet(() -> new NutritionPreference(userId, activity));
        preference.update(activity); preferences.save(preference); return new NutritionDto.Preference(activity);
    }
    @Transactional(readOnly = true)
    public NutritionDto.Recommendation recommendation(Long userId, LocalDate date) {
        var profile = profiles.findByUserId(userId).orElse(null);
        var activity = preference(userId).activityLevel();
        var measurement = measurements.findTopByUserIdAndDeletedFalseAndMeasuredDateLessThanEqualOrderByMeasuredDateDescIdDesc(userId, date).orElse(null);
        String reason = profile == null ? "프로필을 먼저 등록해주세요." : activity == null ? "활동량을 먼저 선택해주세요."
            : measurement == null ? "조회 날짜 이전의 신체 기록이 필요합니다." : null;
        if (reason == null && Period.between(profile.getBirthDate(), date).getYears() < 19) reason = "권장량 계산은 만 19세 이상 성인에게 제공됩니다.";
        if (reason != null) return RecommendationCalculator.unavailable(date, activity, reason);
        int age = Period.between(profile.getBirthDate(), date).getYears();
        return RecommendationCalculator.calculate(measurement.getWeight(), measurement.getHeight(), age, profile.getGender(), profile.getGoalType(), activity, date, measurement.getMeasuredDate());
    }
}
