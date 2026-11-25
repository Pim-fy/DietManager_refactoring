package com.hwanghj.dietmanager.refactoring.backend.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service.SettingHistoryService;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.dto.UserProfileDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.exception.UserProfileNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

@Service
public class ProfileService {
    private final UserProfileRepository userProfileRepository;
    private final SettingHistoryService settingHistoryService;
    private final UserRepository userRepository;

    public ProfileService(UserProfileRepository userProfileRepository, SettingHistoryService settingHistoryService, UserRepository userRepository) {
        this.userProfileRepository = userProfileRepository;
        this.settingHistoryService = settingHistoryService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDto.Response findProfile(Long userId) {
        UserProfile userProfile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(UserProfileNotFoundException::new);
            // == .orElseThrow(() -> new UserProfileNotFoundException());

        return toProfileResponse(userProfile);
    }

    @Transactional
    public UserProfileDto.Response modifyProfile(
            Long userId,
            UserProfileDto.Request request) {
        // 같은 사용자의 저장을 줄 세워 설정 이력의 처음 저장이 겹치지 않게 한다.
        userRepository.findWithLockById(userId).orElseThrow(UserNotFoundException::new);
        UserProfile userProfile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(UserProfileNotFoundException::new);

        GoalType previousGoal = userProfile.getGoalType();
        userProfile.updateProfile(
                request.getGender(),
                request.getBirthDate(),
                request.getGoalType());
        // 목표가 바뀌면 날짜별 설정 이력에 남겨 과거 권장량이 당시 목표로 계산되게 한다.
        settingHistoryService.goalChanged(userProfile, previousGoal);

        return toProfileResponse(userProfile);
    }

    private UserProfileDto.Response toProfileResponse(UserProfile userProfile) {
        return UserProfileDto.Response
                .builder()
                .userId(userProfile.getUser().getId())
                .gender(userProfile.getGender())
                .birthDate(userProfile.getBirthDate())
                .goalType(userProfile.getGoalType())
                .build();
    }
}
