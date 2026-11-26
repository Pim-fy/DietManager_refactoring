package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.NutritionPreference;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.NutritionPreferenceRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.SettingHistoryRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.UserProfile;

/**
 * 목표·활동량이 바뀔 때 날짜별 이력을 남기고, 조회 날짜에 적용 중이던 설정을 찾는다.
 * 이력을 남기는 메서드는 호출한 쪽이 먼저 users 행을 잠근 트랜잭션 안에서 부른다({@code UserRepository.findWithLockById}).
 */
@Service
public class SettingHistoryService {
    private final SettingHistoryRepository histories;
    private final NutritionPreferenceRepository preferences;
    private final Clock clock;

    public SettingHistoryService(SettingHistoryRepository histories, NutritionPreferenceRepository preferences, Clock clock) {
        this.histories = histories;
        this.preferences = preferences;
        this.clock = clock;
    }

    /** 프로필 목표가 바뀌었을 때. 호출 시점의 활동량을 함께 남긴다. */
    @Transactional
    public void goalChanged(UserProfile profile, GoalType previousGoal) {
        if (previousGoal == profile.getGoalType())
            return;
        Long userId = profile.getUser().getId();
        ActivityLevel activity = preferences.findById(userId).map(NutritionPreference::getActivityLevel).orElse(null);
        record(userId, baselineDate(profile), previousGoal, activity, profile.getGoalType(), activity);
    }

    /** 활동량이 바뀌었을 때. 호출 시점의 목표를 함께 남긴다. */
    @Transactional
    public void activityChanged(UserProfile profile, ActivityLevel previousActivity, ActivityLevel activity) {
        if (previousActivity == activity)
            return;
        GoalType goal = profile.getGoalType();
        record(profile.getUser().getId(), baselineDate(profile), goal, previousActivity, goal, activity);
    }

    /** 사용자의 전체 설정 이력(적용일 오름차순). 날짜별 적용 설정은 {@link RecommendationTimeline}이 고른다. */
    @Transactional(readOnly = true)
    public List<SettingHistory> all(Long userId) {
        return histories.findByUserIdOrderByEffectiveDateAsc(userId);
    }

    /**
     * 오늘 날짜 이력을 저장한다. 첫 변경이면 바뀌기 전 설정을 프로필 생성일 기준으로 먼저 남겨
     * 그 사이 날짜가 새 설정으로 계산되지 않게 한다.
     */
    private void record(Long userId, LocalDate baseline, GoalType previousGoal, ActivityLevel previousActivity, GoalType goal, ActivityLevel activity) {
        LocalDate today = LocalDate.now(clock);
        if (!histories.existsByUserId(userId) && baseline.isBefore(today)) {
            histories.save(new SettingHistory(userId, baseline, previousGoal, previousActivity));
        }
        SettingHistory todays = histories.findByUserIdAndEffectiveDate(userId, today)
                .orElseGet(() -> new SettingHistory(userId, today, goal, activity));
        todays.update(goal, activity);
        histories.save(todays);
    }

    private LocalDate baselineDate(UserProfile profile) {
        return profile.getCreatedAt() == null ? LocalDate.now(clock) : profile.getCreatedAt().toLocalDate();
    }
}
