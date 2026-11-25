package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto.NutritionDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.NutritionPreference;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.NutritionPreferenceRepository;
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
        return timeline(userId, date).at(date);
    }

    /**
     * end 날짜까지의 권장량 계산 재료(프로필, 설정 이력, end 이전 신체 기록)를 각각 한 번씩 조회한다.
     * 날짜별 계산은 메모리에서 하므로 조회 횟수가 기간 길이와 관계없다.
     */
    @Transactional(readOnly = true)
    public RecommendationTimeline timeline(Long userId, LocalDate end) {
        var profile = profiles.findByUserId(userId).orElse(null);
        List<SettingHistory> settings = histories.all(userId);
        // 설정 이력이 하나도 없을 때만 현재 활동량을 쓴다.
        ActivityLevel current = settings.isEmpty() ? preference(userId).getActivityLevel() : null;
        var bodies = measurements.findByUserIdAndDeletedFalseAndMeasuredDateLessThanEqualOrderByMeasuredDateAscIdAsc(userId, end);
        return new RecommendationTimeline(profile, settings, current, bodies);
    }
}
