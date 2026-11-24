package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto.NutritionDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.NutritionPreference;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.NutritionPreferenceRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

@Service
public class NutritionService {
    private final NutritionPreferenceRepository preferences;
    private final UserProfileRepository profiles;
    private final BodyMeasurementRepository measurements;
    private final SettingHistoryService histories;
    private final UserRepository users;

    public NutritionService(NutritionPreferenceRepository preferences, UserProfileRepository profiles, BodyMeasurementRepository measurements,
            SettingHistoryService histories, UserRepository users) {
        this.preferences = preferences;
        this.profiles = profiles;
        this.measurements = measurements;
        this.histories = histories;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public NutritionDto.Preference preference(Long userId) {
        return NutritionDto.Preference.builder()
                .activityLevel(preferences.findById(userId).map(NutritionPreference::getActivityLevel).orElse(null))
                .build();
    }

    @Transactional
    public NutritionDto.Preference save(Long userId, ActivityLevel activity) {
        // 같은 사용자의 저장을 줄 세워 활동량·설정 이력의 처음 저장이 겹치지 않게 한다.
        users.findWithLockById(userId).orElseThrow(UserNotFoundException::new);
        var existing = preferences.findById(userId);
        ActivityLevel previous = existing.map(NutritionPreference::getActivityLevel).orElse(null);
        var preference = existing.orElseGet(() -> new NutritionPreference(userId, activity));
        preference.update(activity);
        preferences.save(preference);
        profiles.findByUserId(userId).ifPresent(profile -> histories.activityChanged(profile, previous, activity));
        return NutritionDto.Preference.builder().activityLevel(activity).build();
    }

    @Transactional(readOnly = true)
    public NutritionDto.Recommendation recommendation(Long userId, LocalDate date) {
        var profile = profiles.findByUserId(userId).orElse(null);
        // 조회 날짜 당시의 목표·활동량. 이력이 없으면 현재 설정을 쓴다.
        var setting = histories.at(userId, date);
        var activity = setting.isPresent() ? setting.get().getActivityLevel() : preference(userId).getActivityLevel();
        var measurement = measurements.findTopByUserIdAndDeletedFalseAndMeasuredDateLessThanEqualOrderByMeasuredDateDescIdDesc(userId, date).orElse(null);
        String reason = profile == null ? "프로필을 먼저 등록해주세요."
                : activity == null ? "활동량을 먼저 선택해주세요."
                        : measurement == null ? "조회 날짜 이전의 신체 기록이 필요합니다." : null;
        if (reason == null && Period.between(profile.getBirthDate(), date).getYears() < 19)
            reason = "권장량 계산은 만 19세 이상 성인에게 제공됩니다.";
        if (reason != null)
            return RecommendationCalculator.unavailable(date, activity, reason);
        int age = Period.between(profile.getBirthDate(), date).getYears();
        GoalType goal = setting.map(SettingHistory::getGoalType).orElse(profile.getGoalType());
        return RecommendationCalculator.calculate(measurement.getWeight(), measurement.getHeight(), age, profile.getGender(), goal, activity,
                date, measurement.getMeasuredDate());
    }
}
