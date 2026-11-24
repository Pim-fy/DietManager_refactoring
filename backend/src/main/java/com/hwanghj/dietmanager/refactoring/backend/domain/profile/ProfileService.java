package com.hwanghj.dietmanager.refactoring.backend.domain.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.exception.UserProfileNotFoundException;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserProfileRepository;
@Service
public class ProfileService {
    private final UserProfileRepository userProfileRepository;
    public ProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }
    @Transactional(readOnly = true)
    public UserProfileDto.Response findProfile(Long userId){
        UserProfile userProfile = userProfileRepository
            .findByUserId(userId)
            .orElseThrow(UserProfileNotFoundException::new);
            // == .orElseThrow(() -> new UserProfileNotFoundException());
        
        return toProfileResponse(userProfile);
    }

    @Transactional
    public UserProfileDto.Response modifyProfile(
            Long userId,
            UserProfileDto.Request request){
        UserProfile userProfile = userProfileRepository
            .findByUserId(userId)
            .orElseThrow(UserProfileNotFoundException::new);

        userProfile.updateProfile(
            request.getGender(), 
            request.getBirthDate(), 
            request.getGoalType());
        
        return toProfileResponse(userProfile);
    }

    private UserProfileDto.Response toProfileResponse(UserProfile userProfile){
        return UserProfileDto.Response
            .builder()
            .userId(userProfile.getUser().getId())
            .gender(userProfile.getGender())
            .birthDate(userProfile.getBirthDate())
            .goalType(userProfile.getGoalType())
            .build();
    }
}
