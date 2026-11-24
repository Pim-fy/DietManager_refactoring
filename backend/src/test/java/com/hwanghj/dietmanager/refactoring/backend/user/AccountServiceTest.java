package com.hwanghj.dietmanager.refactoring.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import com.hwanghj.dietmanager.refactoring.backend.user.AuthenticationService;
import com.hwanghj.dietmanager.refactoring.backend.user.AccountService;
import com.hwanghj.dietmanager.refactoring.backend.profile.ProfileService;
import com.hwanghj.dietmanager.refactoring.backend.measurement.BodyMeasurementService;
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

import com.hwanghj.dietmanager.refactoring.backend.common.Gender;
import com.hwanghj.dietmanager.refactoring.backend.common.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.common.UserRole;
import com.hwanghj.dietmanager.refactoring.backend.measurement.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.user.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.user.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.user.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.user.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.profile.UserProfileDto;
import com.hwanghj.dietmanager.refactoring.backend.user.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.measurement.BodyMeasurement;
import com.hwanghj.dietmanager.refactoring.backend.user.User;
import com.hwanghj.dietmanager.refactoring.backend.user.UserProfile;
import com.hwanghj.dietmanager.refactoring.backend.exception.BodyMeasurementNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.exception.InvalidLoginException;
import com.hwanghj.dietmanager.refactoring.backend.exception.InvalidPasswordException;
import com.hwanghj.dietmanager.refactoring.backend.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.exception.UserProfileNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.measurement.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.profile.UserProfileRepository;
import com.hwanghj.dietmanager.refactoring.backend.user.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    private AccountService accountService;
    @BeforeEach void setUp() { accountService = new AccountService(userRepository, passwordEncoder); }
    @Test
    void concurrentEmailConflictUsesDomainException() {
        User user = User.builder().email("old@example.test").passwordHash("hash").userName("사용자").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.test")).thenReturn(false);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("unique email"))
            .when(userRepository).flush();
        var request = UserAccountModifyDto.Request.builder().email("new@example.test").userName("사용자").build();
        assertThatThrownBy(() -> accountService.modifyAccount(1L, request)).isInstanceOf(DuplicateEmailException.class);
    }
    @Test
    void findAcocuntReturnsAccountWhenUserExists() {
        // given: user 객체 생성.
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();
        
        // given: 테스트용으로 private 필드인 id값을 강제로 넣음.
        ReflectionTestUtils.setField(user, "id", 1L);

        // given: 로그인 성공 설정.
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        // when: 테스트 실행.
        UserAccountFindDto.Response response = accountService.findAccount(1L);

        // then: 응답 값 검증.
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUserName()).isEqualTo("홍길동");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);

        // then: 계정 조회 로직 실행 확인.
        verify(userRepository).findById(1L);
    }
    @Test
    void findAccountthrowsExceptioniWhenUserDoesNotExist() {
        // given: 조회된 결과가 없음을 설정.
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> accountService.findAccount(1L))
            .isInstanceOf(UserNotFoundException.class)
            // .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");

        // then
        verify(userRepository).findById(1L);
    }
    @Test
    void modifyAccountUpdatesAccountWhenRequestIsValid() {
        // given: 기존 user 객체 생성.
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        // given: 계정 수정 요청 DTO 생성.
        UserAccountModifyDto.Request request = createModifyAccountRequest(
            "new@example.com",
            "김철수");

        // given: userId로 사용자 조회 성공.
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // given: 변경할 이메일이 중복되지 않은 상황.
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // when: 계정 정보 수정 실행.
        UserAccountModifyDto.Response response = accountService.modifyAccount(1L, request);

        // then: 응답 값 검증.
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getUserName()).isEqualTo("김철수");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);

        // then: 엔터티 값 변경 확인.
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getUserName()).isEqualTo("김철수");

        // then: 조회와 중복 검사 실행 확인.
        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("new@example.com");
    }
    @Test
    void modifyAccountDoesNotCheckDuplicateEmailWhenEmailIsNotChanged() {
        // given: 기존 user 객체 생성.
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        // given: 이메일은 그대로, 사용자 이름만 변경하는 요청.
        UserAccountModifyDto.Request request = createModifyAccountRequest(
            "test@example.com",
            "김철수");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserAccountModifyDto.Response response = accountService.modifyAccount(1L, request);

        // then
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUserName()).isEqualTo("김철수");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getUserName()).isEqualTo("김철수");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
    }
    @Test
    void modifyAccountThrowsDuplicateEmailExceptionWhenNewEmailAlreadyExists() {
        // given
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        UserAccountModifyDto.Request request = createModifyAccountRequest(
            "new@example.com",
            "김철수");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        // when + then
        assertThatThrownBy(() -> accountService.modifyAccount(1L, request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 사용 중인 이메일입니다.");

        // then: 예외 발생 시 기존 값 유지 확인.
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getUserName()).isEqualTo("홍길동");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("new@example.com");
    }
    @Test
    void modifyAccountThrowsExceptionWhenUserDoesNotExist() {
        // given
        UserAccountModifyDto.Request request = createModifyAccountRequest(
            "new@example.com",
            "김철수");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> accountService.modifyAccount(1L, request))
            .isInstanceOf(UserNotFoundException.class)
            // .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
    }
    @Test
    void changePasswordUpdatesPasswordWhenCurrentPasswordMatches() {
        // given
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("old-hashed-password")
            .userName("홍길동")
            .build();

        // given: 비밀번호 변경 요청 DTO 생성
        PasswordChangeDto.Request request = createPasswordChangeRequest("old-password", "new-password123");

        // given: userId로 사용자 조회 성공. id가 1L인 사용자가 존재.
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // given: 현재 비밀번호를 올바르게 입력.
        when(passwordEncoder.matches("old-password", "old-hashed-password")).thenReturn(true);
        // given: 새 비밀번호 해싱.
        when(passwordEncoder.encode("new-password123")).thenReturn("new-hashed-password");

        // when: 비밀번호 변경 서비스 메서드 실행.
        PasswordChangeDto.Response response = accountService.changePassword(1L, request);

        // then: 응답 메시지가 기대값과 같은지 확인
        assertThat(response.getMessage()).isEqualTo("비밀번호가 변경되었습니다.");
        // then: 비밀번호 해시가 새 해시값으로 바뀌었는지 확인
        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-password");

        // then: userId로 사용자를 조회했는지 확인.
        verify(userRepository).findById(1L);
        // then: 입력한 비밀번호와 저장된 해시값을 비교했는지 확인.
        verify(passwordEncoder).matches("old-password", "old-hashed-password");
        // then: 새 비밀번호 저장 전 해싱했는지 확인.
        verify(passwordEncoder).encode("new-password123");
    }
    @Test
    void changePasswordThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        // given: 비밀번호 변경 요청 DTO 생성
        PasswordChangeDto.Request request = createPasswordChangeRequest("old-password", "new-password123");

        // given: ID로 조회 시 사용자 없음
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> accountService.changePassword(1L, request))       // when: 비밀번호 변경 서비스 메서드 실행
            .isInstanceOf(UserNotFoundException.class)      // then: 발생한 예외의 타입이 UserNotFoundException 인지 확인
            .hasMessage("사용자를 찾을 수 없습니다.");      // then: 발생한 예외의 메시지가 "사용자를 찾을 수 없습니다." 인지 확인
        
        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }
    @Test
    void changePasswordThrowsInvalidPasswordExceptionWhenCurrentPasswordDoesNotMatch() {
        // given
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("old-hashed-password")
            .userName("홍길동")
            .build();

        PasswordChangeDto.Request request = createPasswordChangeRequest("wrong-password", "new-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hashed-password")).thenReturn(false);

        // when + then
        assertThatThrownBy(() -> accountService.changePassword(1L, request))
            .isInstanceOf(InvalidPasswordException.class)
            .hasMessage("현재 비밀번호가 일치하지 않습니다.");
        
        assertThat(user.getPasswordHash()).isEqualTo("old-hashed-password");

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrong-password", "old-hashed-password");
        verify(passwordEncoder, never()).encode(anyString());

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
