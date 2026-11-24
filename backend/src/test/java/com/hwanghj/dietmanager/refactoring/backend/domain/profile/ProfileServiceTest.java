package com.hwanghj.dietmanager.refactoring.backend.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.AuthenticationService;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.AccountService;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.ProfileService;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.BodyMeasurementService;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.Gender;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserRole;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.UserProfileDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.BodyMeasurement;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception.BodyMeasurementNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidLoginException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidPasswordException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.exception.UserProfileNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private UserProfileRepository userProfileRepository;
    private ProfileService profileService;
    @BeforeEach void setUp() { profileService = new ProfileService(userProfileRepository); }
    @Test
    void findProfileReturnsProfileWhenProfileExists() {
        // given
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        UserProfile profile = UserProfile.builder()
            .gender(Gender.MALE)
            .birthDate(LocalDate.of(1995, 1, 1))
            .goalType(GoalType.LOSE_WEIGHT)
            .build();

        user.assignProfile(profile);

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        // when
        UserProfileDto.Response response = profileService.findProfile(1L);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getGender()).isEqualTo(Gender.MALE);
        assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
        assertThat(response.getGoalType()).isEqualTo(GoalType.LOSE_WEIGHT);

        verify(userProfileRepository).findByUserId(1L);
    }
    @Test
    void findProfileThrowsUserProfileNotFoundExceptionWhenProfileDoesNotExist() {
        // given
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> profileService.findProfile(1L))
            .isInstanceOf(UserProfileNotFoundException.class)
            .hasMessage("사용자 프로필을 찾을 수 없습니다.");

        verify(userProfileRepository).findByUserId(1L);
    }
    @Test
    void modifyProfileUpdatesProfileWhenProfileExists() {
        // given
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        UserProfile profile = UserProfile.builder()
            .gender(Gender.MALE)
            .birthDate(LocalDate.of(1995, 1, 1))
            .goalType(GoalType.LOSE_WEIGHT)
            .build();

        user.assignProfile(profile);

        UserProfileDto.Request request = createProfileRequest(
            Gender.FEMALE,
            LocalDate.of(1998, 5, 10),
            GoalType.MAINTAIN_WEIGHT);

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        // when
        UserProfileDto.Response response = profileService.modifyProfile(1L, request);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(1998, 5, 10));
        assertThat(response.getGoalType()).isEqualTo(GoalType.MAINTAIN_WEIGHT);

        assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1998, 5, 10));
        assertThat(profile.getGoalType()).isEqualTo(GoalType.MAINTAIN_WEIGHT);

        verify(userProfileRepository).findByUserId(1L);
    }
    @Test
    void modifyProfileThrowsUserProfileNotFoundExceptionWhenProfileDoesNotExist() {
        // given
        UserProfileDto.Request request = createProfileRequest(
            Gender.FEMALE,
            LocalDate.of(1998, 5, 10),
            GoalType.MAINTAIN_WEIGHT);

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> profileService.modifyProfile(1L, request))
            .isInstanceOf(UserProfileNotFoundException.class)
            .hasMessage("사용자 프로필을 찾을 수 없습니다.");

        verify(userProfileRepository).findByUserId(1L);
    }
    private UserRegisterDto.Request createRegisterRequest() throws Exception {
        return UserRegisterDto.Request.builder()
                .email("test@example.com")
                .password("password123")
                .userName("홍길동")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 1, 1))
                .goalType(GoalType.LOSE_WEIGHT)
                .build();
    }
    private UserLoginDto.Request createLoginRequest() {
        return UserLoginDto.Request
            .builder()
            .email("test@example.com")
            .password("password123")
            .build();
    }
    private UserAccountModifyDto.Request createModifyAccountRequest(String email, String userName) {
        return UserAccountModifyDto.Request
            .builder()
            .email(email)
            .userName(userName)
            .build();
    }
    private PasswordChangeDto.Request createPasswordChangeRequest(String currentPassword,String newPassword) {
        return PasswordChangeDto.Request.builder()
            .currentPassword(currentPassword)
            .newPassword(newPassword)
            .build();
    }
    private UserProfileDto.Request createProfileRequest(
            Gender gender,
            LocalDate birthDate,
            GoalType goalType) {
        return UserProfileDto.Request.builder()
            .gender(gender)
            .birthDate(birthDate)
            .goalType(goalType)
            .build();
    }
    private BodyMeasurementDto.Request createBodyMeasurementRequest(
            LocalDate measuredDate,
            Double height,
            Double weight) {
        return BodyMeasurementDto.Request.builder()
            .measuredDate(measuredDate)
            .height(height)
            .weight(weight)
            .build();
    }
    private User createUserWithId(Long userId) {
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        ReflectionTestUtils.setField(user, "id", userId);

        return user;
    }
    private BodyMeasurement createBodyMeasurement(
            Long measurementId,
            User user,
            LocalDate measuredDate,
            Double height,
            Double weight) {
        BodyMeasurement bodyMeasurement = BodyMeasurement.builder()
            .user(user)
            .measuredDate(measuredDate)
            .height(height)
            .weight(weight)
            .build();

        ReflectionTestUtils.setField(bodyMeasurement, "id", measurementId);

        return bodyMeasurement;
    }
}
