package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.entity.BodyMeasurement;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto.NutritionDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.UserProfile;

/**
 * 날짜별 권장량 계산 규칙을 한 곳에 모은 클래스.
 * 프로필·설정 이력·신체 기록을 한 번 받아 두고, 날짜마다 그날 적용 중이던 설정과 그 날짜 이전 최신 신체 기록을 메모리에서 고른다.
 * 단일 날짜 권장량 API와 기간 통계가 모두 이 클래스로 계산한다.
 */
public class RecommendationTimeline {
    private final UserProfile profile;
    private final TreeMap<LocalDate, SettingHistory> settings = new TreeMap<>();
    private final ActivityLevel currentActivity;
    private final TreeMap<LocalDate, BodyMeasurement> measurements = new TreeMap<>();

    /**
     * @param profile 프로필. 없으면 null
     * @param histories 사용자의 전체 설정 이력
     * @param currentActivity 현재 활동량. 설정 이력이 하나도 없을 때만 쓴다
     * @param bodies 계산할 마지막 날짜 이전의 삭제되지 않은 신체 기록. 측정일·id 오름차순
     */
    public RecommendationTimeline(UserProfile profile, List<SettingHistory> histories, ActivityLevel currentActivity, List<BodyMeasurement> bodies) {
        this.profile = profile;
        this.currentActivity = currentActivity;
        for (SettingHistory history : histories)
            settings.put(history.getEffectiveDate(), history);
        // 같은 측정일이 여러 개면 id가 큰(나중에 저장한) 기록이 남는다.
        for (BodyMeasurement body : bodies)
            measurements.put(body.getMeasuredDate(), body);
    }

    /** 조회 날짜의 권장량. 계산할 수 없으면 available=false와 이유를 담는다. */
    public NutritionDto.Recommendation at(LocalDate date) {
        SettingHistory setting = settingAt(date);
        // 이력이 있으면 그 이력의 활동량(미선택이면 null), 이력이 없으면 현재 설정을 쓴다.
        ActivityLevel activity = setting != null ? setting.getActivityLevel() : currentActivity;
        Map.Entry<LocalDate, BodyMeasurement> latest = measurements.floorEntry(date);
        BodyMeasurement measurement = latest == null ? null : latest.getValue();
        String reason = profile == null ? "프로필을 먼저 등록해주세요."
                : activity == null ? "활동량을 먼저 선택해주세요."
                        : measurement == null ? "조회 날짜 이전의 신체 기록이 필요합니다." : null;
        if (reason == null && Period.between(profile.getBirthDate(), date).getYears() < 19)
            reason = "권장량 계산은 만 19세 이상 성인에게 제공됩니다.";
        if (reason != null)
            return RecommendationCalculator.unavailable(date, activity, reason);
        int age = Period.between(profile.getBirthDate(), date).getYears();
        GoalType goal = setting != null ? setting.getGoalType() : profile.getGoalType();
        return RecommendationCalculator.calculate(measurement.getWeight(), measurement.getHeight(), age, profile.getGender(), goal, activity,
                date, measurement.getMeasuredDate());
    }

    /** 조회 날짜에 적용 중이던 설정. 모든 이력보다 이전 날짜면 가장 오래된 이력, 이력이 없으면 null. */
    private SettingHistory settingAt(LocalDate date) {
        Map.Entry<LocalDate, SettingHistory> entry = settings.floorEntry(date);
        if (entry == null)
            entry = settings.firstEntry();
        return entry == null ? null : entry.getValue();
    }
}
