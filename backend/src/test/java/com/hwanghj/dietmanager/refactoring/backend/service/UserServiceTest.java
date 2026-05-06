package com.hwanghj.dietmanager.refactoring.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.hwanghj.dietmanager.refactoring.backend.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.exception.InvalidLoginException;
import com.hwanghj.dietmanager.refactoring.backend.repository.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.security.JwtTokenProvider;

// 회원가입 서비스 로직 검증.
// Spring 전체를 띄우지 않고 가짜 객체인 mock으로 대체
// Mockito 테스트를 쓰겠다는 설정
    // Mockito: 가짜 객체(Mock)을 만들고 관리할 수 있게 해주는 테스트 프레임워크.
    // Mock 객체 생성 -> 행동 정의 -> 검증
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // 실제 DB를 사용하지 않고, 가짜 객체를 만듦
    @Mock
    private UserRepository userRepository;
    
    // 실제 BCrypt 해싱을 하지 않고, 원하는 해싱 결과를 반환하도록 제어하기 위한 mock
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    // 테스트 대상인 UserService 생성
    private UserService userService;

    // 각 테스트 메서드 실행 전 매번 실행
    @BeforeEach
    void setUp() {
        // UserService 객체 생성
        userService = new UserService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    // 회원가입 로직 테스트.

    // 테스트1: 정상 회원가입 흐름 검증 테스트.
    // 정상적인 회원가입 요청이 들어왔을 때 User와 UserProfile이 제대로 만들어지는지 확인.
    // throws 사용으로 예외 발생 시 테스트 실패 처리
    @Test
    void registerCreatesUserWithProfile() throws Exception {

        // given: 테스트 준비

        // 테스트용 회원가입 요청 DTO 생성.
        // 현재 파일에 정의된 메서드 사용.
        UserRegisterDto.Request request = createRegisterRequest();

        // 이미 가입된 이메일이 아니라는 상황을 생성.
            // userRepository.existsByEmail("test@example.com") 호출 -> false반환
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        
        // 테스트에서는 해싱된 비밀번호 값을 고정된 값을 반환하게 사용함.
            // passwordEncoder.encode("password123") 호출 -> "hashed-password" 반환
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        // when: 테스트 진행

        // 실제 테스트 대상 userService.register() 호출
        // 여기서 회원가입 로직이 전부 실행됨.
        UserRegisterDto.Response response = userService.register(request);

        // then: 검증 시작

        // 회원가입 성공 응답 메시지가 기대값인지 확인.
            // 로직에서의 성공 응답 메시지: "회원가입이 완료되었습니다."
        assertThat(response.getMessage()).isEqualTo("회원가입이 완료되었습니다.");

        // userRepository.save(user)에 전달된 User 객체를 잡아오기 위한 도구
        // User 객체를 테스트에서 직접 확인.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // userRepository.save()가 실제로 호출되었는지 확인.
        // 호출 당시 전달된 인자를 userCaptor에 저장.
        verify(userRepository).save(userCaptor.capture());

        // save()에 전달된 user객체를 꺼냄.
            // 실제 DB에 저장된 값이 아님.
            // 저장하려고 넘긴 객체.
        User savedUser = userCaptor.getValue();

        // 생성된 user의 이메일이 요청값과 같은지 확인.
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        // 생성된 user의 비밀번호가 원문이 아니라 해싱 결과가 들어갔는지 확인.
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        // UserName이 요청값과 같은지 확인.
        assertThat(savedUser.getUserName()).isEqualTo("홍길동");
        // 회원가입 시 기본 권한이 USER로 설정되었는지 확인.
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        // UserProfile이 연결되었는지 확인
        assertThat(savedUser.getProfile()).isNotNull();
        // 양방향 연관관계 확인.
        // user.getProfile()도 있고, profile.getUser()도 다시 같은 user를 가리켜야 함.
        assertThat(savedUser.getProfile().getUser()).isSameAs(savedUser);
        // 성별이 요청값과 같은지 확인.
        assertThat(savedUser.getProfile().getGender()).isEqualTo(Gender.MALE);
        // 생년월일이 요청값과 같은지 확인.
        assertThat(savedUser.getProfile().getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
        // 목표 유형이 요청값과 같은지 확인.
        assertThat(savedUser.getProfile().getGoalType()).isEqualTo(GoalType.LOSE_WEIGHT);
    }

    // 테스트2: 이메일 중복 선검사 테스트.
    // 이미 가입된 이메일이면 회원가입 중단, DuplicateEmailException 발생 확인.
    // throws 사용으로 예외 발생 시 테스트 실패 처리.
    @Test
    void registerThrowsDuplicateEmailExceptionWhenEmailAlreadyExists() throws Exception {

        // given: 테스트 준비

        // 테스트용 회원가입 요청 DTO 생성.
        // 아래 정의된 메서드 사용.
        UserRegisterDto.Request request = createRegisterRequest();

        // 이미 가입된 이메일이 있다는 상황 생성.
        // userRepository.existsByEmail("test@example.com") 호출 -> true 반환.
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // 예외 테스트이므로 when / then 동시

        // userService.register(request) 호출 시 예외가 발생해야 한다는 검증.
        assertThatThrownBy(() -> userService.register(request))
                // 발생한 예외 타입이 DuplicateEmailException 인지 확인.
                .isInstanceOf(DuplicateEmailException.class)
                // 예외 메시지가 기대값인지 확인.
                .hasMessage("이미 사용 중인 이메일입니다.");

        // 이메일 중복 시 비밀번호 해싱 진행되면 안됨.
        // passwordEncoder.encode(...)가 한 번도 호출되지 않았는지 확인함.
            // never()이 0번 호출을 의미.
        verify(passwordEncoder, never()).encode(any());

        // 이메일 중복 시 저장 로직까지 진행되면 안됨.
        // userRepository.save(...)가 한 번도 호출되지 않았는지 확인함.
        verify(userRepository, never()).save(any());
    }

    // 테스트3: 저장 시점에 DB unique 제약이 터지는 상황 검증.
    // existsByEmail()은 통과, 저장 시점의 DB unique 제약.
    // throws사용으로 예외 발생 시 테스트 실패 처리.
    @Test
    void registerConvertsDataIntegrityViolationToDuplicateEmailException() throws Exception {

        // given : 테스트 준비

        // 테스트용 회원가입 요청 DTO 생성.
        // 현재 파일에 정의된 메서드 사용.
        UserRegisterDto.Request request = createRegisterRequest();

        // 이미 가입된 이메일이 아니라는 상황 생성.
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        // 테스트에서는 해싱된 비밀번호 값을 고정된 값을 반환하게 사용.
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        // userRepository.save() 호출 시 DataIntegrityViolationException을 던지게 설정함.
            // 실제 DB에서 unique 제약 위반이 난 상황을 mock으로 만든 것.
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        // when + then: 예외 테스트이므로 동시에 진행됨.
        
        // userService.register(request) 호출 시 예외가 발생해야 한다는 검증.
        // 서비스가 예외를 그대로 밖으로 내보내지 않고, DuplicateEmailException으로 변환하는지 확인.
        assertThatThrownBy(() -> userService.register(request))
                // 발생한 예외 타입이 DuplicateEmailException인지 확인.
                .isInstanceOf(DuplicateEmailException.class)
                // 예외 메시지가 기대값인지 확인.
                .hasMessage("이미 사용 중인 이메일입니다.");
    }

    // 회원가입 요청 DTO 생성.
    // 빌더 패턴 사용.
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

    // 로그인 로직 테스트.

    // 테스트1: 로그인 성공.
    @Test
    void loginReturnsUserInfoWhenCredentialsAreValid() throws Exception{
        // given

        // 테스트용 요청 DTO 생성.
        UserLoginDto.Request request = createLoginRequest();

        // user 객체 생성.
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();

        // request의 email과 동일 email이 DB에 존재하는 상황 생성.
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));
        
        // given: 지정한 형태의 토큰 반황 상황 설정.
        when(jwtTokenProvider.createAccessToken(user))
            .thenReturn("access-token");
        
        // requset의 원본 password를 동일 조건으로 해싱한 값이 DB에 저장된 해싱된 비밀번호와 일치하는 상황 생성.
        when(passwordEncoder.matches("password123", "hashed-password"))
            .thenReturn(true);
            
        // when: 테스트 진행.

        // 로그인 로직 실행.
        UserLoginDto.Response response = userService.login(request);

        // then: 테스트 검증.

        // 응답의 email이 기대값과 동일한지 확인.
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        // 응답의 userName이 기대값과 동일한지 확인.
        assertThat(response.getUserName()).isEqualTo("홍길동");
        // 응답의 UserRole이 기본값인 USER가 맞는지 확인.
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        // then: 응답의 토큰 확인.
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        // then: 토큰 생성 로직 실행 확인.
        verify(jwtTokenProvider).createAccessToken(user);
        
    }
    // 테스트2: DB에 존재하지 않는 이메일로 로그인 시도.
    @Test 
    void loginThrowsInvalidLoginExceptionWhenEmailDoesNotExist() throws Exception{
        // given

        // 로그인 요청 DTO 생성.
        UserLoginDto.Request request = createLoginRequest();

        // DB에 존재하지 않는 이메일 로그인 시도 상황 설정.
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.empty());

        // when + then.

        // when: 로그인 API에 로그인 요청.
        assertThatThrownBy(() -> userService.login(request))
            // then: 발생한 예외 타입이 InvalidLoginException인지 확인.
            .isInstanceOf(InvalidLoginException.class)
            // then: 예외 메시지가 기대값인지 확인.
            .hasMessage("이메일 또는 비밀번호가 일치하지 않습니다.");
        
        // then: 비밀번호 검증 로직 실행 확인
        // passwordEncoder.matches()가 한번도 실행되지 않았는지 확인.
            // never()이 0번 호출을 의미.   
        verify(passwordEncoder, never()).matches(any(), any());
        // then: 토큰 생성 로직이 한번도 실행되지 않았는지 확인.
        verify(jwtTokenProvider, never()).createAccessToken(any(User.class));
    }

    // 테스트3: 비밀번호 불일치.
    @Test
    void loginThrowsInvalidLoginExceptionWhenPasswordDoesNotMatch() throws Exception{
        // given

        // 로그인 요청 DTO 생성.
        UserLoginDto.Request request = createLoginRequest();

        // user 객체 생성.
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();
        
        // DB에 존재하는 이메일 로그인 시도 상황 설정.
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // 비밀번호 불일치 상황 설정.
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);

        // when + then

        // when: 로그인 API에 로그인 요청.
        assertThatThrownBy(() -> userService.login(request))
            .isInstanceOf(InvalidLoginException.class)  // then: 발생한 예외 타입이 InvalidLoginException인지 확인.
            .hasMessage("이메일 또는 비밀번호가 일치하지 않습니다.");   // then: 예외 메시지가 기대값인지 확인.

        // then: 토큰 생성 로직이 한번도 실행되지 않았는지 확인.
        verify(jwtTokenProvider, never()).createAccessToken(any(User.class));
    }

    // 로그인 요청 DTO 생성.
    private UserLoginDto.Request createLoginRequest() {
        return UserLoginDto.Request
            .builder()
            .email("test@example.com")
            .password("password123")
            .build();
    }


    // 계정 조회 서비스 테스트.

    // 테스트1 : 계정 조회 성공.
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
        UserAccountFindDto.Response response = userService.findAccount(1L);

        // then: 응답 값 검증.
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUserName()).isEqualTo("홍길동");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);

        // then: 계정 조회 로직 실행 확인.
        verify(userRepository).findById(1L);
    }

    // 테스트2: 계정 조회 실패.
    @Test
    void findAccountthrowsExceptioniWhenUserDoesNotExist() {
        // given: 조회된 결과가 없음을 설정.
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> userService.findAccount(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");

        // then
        verify(userRepository).findById(1L);
    }

    // 계정 정보 수정 서비스 테스트.

    // 테스트1: 이메일과 사용자 이름 수정 성공.
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
        UserAccountModifyDto.Response response = userService.modifyAccount(1L, request);

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

    // 테스트2: 이메일이 기존과 같으면 이메일 중복 검사를 하지 않고 사용자 이름만 수정.
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
        UserAccountModifyDto.Response response = userService.modifyAccount(1L, request);

        // then
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUserName()).isEqualTo("김철수");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getUserName()).isEqualTo("김철수");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    // 테스트3: 변경하려는 이메일이 이미 사용 중이면 DuplicateEmailException 발생.
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
        assertThatThrownBy(() -> userService.modifyAccount(1L, request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 사용 중인 이메일입니다.");

        // then: 예외 발생 시 기존 값 유지 확인.
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getUserName()).isEqualTo("홍길동");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("new@example.com");
    }

    // 테스트4: 사용자가 존재하지 않으면 IllegalArgumentException 발생.
    @Test
    void modifyAccountThrowsExceptionWhenUserDoesNotExist() {
        // given
        UserAccountModifyDto.Request request = createModifyAccountRequest(
            "new@example.com",
            "김철수");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> userService.modifyAccount(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    // 계정 정보 수정 요청 DTO 생성.
    private UserAccountModifyDto.Request createModifyAccountRequest(String email, String userName) {
        return UserAccountModifyDto.Request
            .builder()
            .email(email)
            .userName(userName)
            .build();
    }
}
