package com.hwanghj.dietmanager.refactoring.backend.domain.measurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
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

import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.dto.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.entity.BodyMeasurement;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception.BodyMeasurementNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.service.BodyMeasurementService;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.dto.UserProfileDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.exception.UserProfileNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.service.ProfileService;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.Gender;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.UserRole;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidLoginException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidPasswordException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.AccountService;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.AuthenticationService;
import com.hwanghj.dietmanager.refactoring.backend.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class BodyMeasurementServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private BodyMeasurementRepository bodyMeasurementRepository;
    private BodyMeasurementService bodyMeasurementService;

    @BeforeEach
    void setUp() {
        bodyMeasurementService = new BodyMeasurementService(userRepository, bodyMeasurementRepository);
    }

    @Test
    void addBodyMeasurementSavesMeasurementWhenUserExists() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashed-password")
                .userName("홍길동")
                .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        BodyMeasurementDto.Request request = createBodyMeasurementRequest(
                LocalDate.of(2026, 7, 1),
                175.5,
                70.2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bodyMeasurementRepository.save(any(BodyMeasurement.class)))
                .thenAnswer(invocation -> {
                    BodyMeasurement bodyMeasurement = invocation.getArgument(0);
                    ReflectionTestUtils.setField(bodyMeasurement, "id", 10L);
                    return bodyMeasurement;
                });

        // when
        BodyMeasurementDto.Response response = bodyMeasurementService.addBodyMeasurement(1L, request);

        // then
        assertThat(response.getMeasurementId()).isEqualTo(10L);
        assertThat(response.getMeasuredDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getHeight()).isEqualTo(175.5);
        assertThat(response.getWeight()).isEqualTo(70.2);

        ArgumentCaptor<BodyMeasurement> bodyMeasurementCaptor = ArgumentCaptor.forClass(BodyMeasurement.class);

        verify(userRepository).findById(1L);
        verify(bodyMeasurementRepository).save(bodyMeasurementCaptor.capture());

        BodyMeasurement savedBodyMeasurement = bodyMeasurementCaptor.getValue();

        assertThat(savedBodyMeasurement.getUser()).isSameAs(user);
        assertThat(savedBodyMeasurement.getMeasuredDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(savedBodyMeasurement.getHeight()).isEqualTo(175.5);
        assertThat(savedBodyMeasurement.getWeight()).isEqualTo(70.2);
    }

    @Test
    void addBodyMeasurementThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        // given
        BodyMeasurementDto.Request request = createBodyMeasurementRequest(
                LocalDate.of(2026, 7, 1),
                175.5,
                70.2);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> bodyMeasurementService.addBodyMeasurement(1L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(1L);
        verify(bodyMeasurementRepository, never()).save(any(BodyMeasurement.class));
    }

    @Test
    void findLatestBodyMeasurementReturnsLatestMeasurementWhenMeasurementExists() {
        // given
        User user = createUserWithId(1L);

        BodyMeasurement bodyMeasurement = createBodyMeasurement(
                10L,
                user,
                LocalDate.of(2026, 7, 1),
                175.5,
                70.2);

        when(bodyMeasurementRepository.findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L))
                .thenReturn(Optional.of(bodyMeasurement));

        // when
        BodyMeasurementDto.Response response = bodyMeasurementService.findLatestBodyMeasurement(1L);

        // then
        assertThat(response.getMeasurementId()).isEqualTo(10L);
        assertThat(response.getMeasuredDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getHeight()).isEqualTo(175.5);
        assertThat(response.getWeight()).isEqualTo(70.2);

        verify(bodyMeasurementRepository).findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L);
    }

    @Test
    void findLatestBodyMeasurementThrowsBodyMeasurementNotFoundExceptionWhenMeasurementDoesNotExist() {
        // given
        when(bodyMeasurementRepository.findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L))
                .thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> bodyMeasurementService.findLatestBodyMeasurement(1L))
                .isInstanceOf(BodyMeasurementNotFoundException.class)
                .hasMessage("신체 측정값을 찾을 수 없습니다.");

        verify(bodyMeasurementRepository).findTopByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L);
    }

    @Test
    void findBodyMeasurementsReturnsMeasurementsWhenMeasurementsExist() {
        // given
        User user = createUserWithId(1L);

        BodyMeasurement latestMeasurement = createBodyMeasurement(
                11L,
                user,
                LocalDate.of(2026, 7, 2),
                175.5,
                70.0);

        BodyMeasurement previousMeasurement = createBodyMeasurement(
                10L,
                user,
                LocalDate.of(2026, 7, 1),
                175.5,
                70.2);

        when(bodyMeasurementRepository.findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L))
                .thenReturn(List.of(latestMeasurement, previousMeasurement));

        // when
        List<BodyMeasurementDto.Response> responses = bodyMeasurementService.findBodyMeasurements(1L);

        // then
        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).getMeasurementId()).isEqualTo(11L);
        assertThat(responses.get(0).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(responses.get(0).getHeight()).isEqualTo(175.5);
        assertThat(responses.get(0).getWeight()).isEqualTo(70.0);

        assertThat(responses.get(1).getMeasurementId()).isEqualTo(10L);
        assertThat(responses.get(1).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(responses.get(1).getHeight()).isEqualTo(175.5);
        assertThat(responses.get(1).getWeight()).isEqualTo(70.2);

        verify(bodyMeasurementRepository).findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L);
    }

    @Test
    void findBodyMeasurementsReturnsEmptyListWhenMeasurementsDoNotExist() {
        // given
        when(bodyMeasurementRepository.findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L))
                .thenReturn(List.of());

        // when
        List<BodyMeasurementDto.Response> responses = bodyMeasurementService.findBodyMeasurements(1L);

        // then
        assertThat(responses).isEmpty();

        verify(bodyMeasurementRepository).findByUserIdAndDeletedFalseOrderByMeasuredDateDescIdDesc(1L);
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

    private PasswordChangeDto.Request createPasswordChangeRequest(String currentPassword, String newPassword) {
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
