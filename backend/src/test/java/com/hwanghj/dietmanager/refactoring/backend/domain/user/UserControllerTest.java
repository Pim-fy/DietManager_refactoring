package com.hwanghj.dietmanager.refactoring.backend.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.service.LoginAttemptLimiter;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.controller.BodyMeasurementController;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.dto.BodyMeasurementDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception.BodyMeasurementNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.service.BodyMeasurementService;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.controller.ProfileController;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.dto.UserProfileDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.exception.UserProfileNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.profile.service.ProfileService;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.controller.AccountController;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.controller.UserController;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.Gender;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.UserRole;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidLoginException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidPasswordException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.AccountService;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.AuthenticationService;
import com.hwanghj.dietmanager.refactoring.backend.global.error.GlobalExceptionHandler;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

// 회원가입 컨트롤러 테스트 코드.
// 컨트롤러 테스트: HTTP응답 확인이 목적.
// Mockito 테스트를 쓰겠다는 설정.
    // Mockito: 가짜 객체(Mock)을 만들고 관리할 수 있게 해주는 테스트 프레임워크.
    // Mock 객체 생성 -> 행동 정의 -> 검증
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    // 컨트롤러가 호출할 서비스를 mock으로 둠.
    @Mock private AuthenticationService authenticationService;
@Mock private AccountService accountService;
@Mock private ProfileService profileService;
@Mock private BodyMeasurementService bodyMeasurementService;

    // MockMvc: 실제 서버를 띄우지 않고도 HTTP 요청처럼 컨트롤러를 테스트하게 해주는 도구.
    private MockMvc mockMvc;

    // 각 테스트 메서드 실행 전 매번 실행.
    @BeforeEach
    void setUp() {
        // MockMvc 객체 생성.
        mockMvc = MockMvcBuilders
                // UserController만 단독 테스트 대상으로 등록.
                // 컨트롤러에 userService를 넣어줌.
                .standaloneSetup(new UserController(authenticationService, new LoginAttemptLimiter()), new AccountController(accountService), new ProfileController(profileService), new BodyMeasurementController(bodyMeasurementService))
                // Spring Security의 @AuthenticationPrincipal 처리기가 자동으로 붙지 않을 수 있기 때문.
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                // 테스트 중 예외 발생 시 실제처럼 GlobalExceptionHandler가 처리하게 등록함.
                    // DuplicateEmailException, MethodArgumentNotValidException 검증 위함.
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    
    // 회원가입 컨트롤러 테스트.

    // 테스트1: 정상 회원가입 요청 테스트.
    // 정상적인 회원가입 요청을 보내면 201 created와 함께 성공 메시지 반환하는지 확인.
    @Test
    void registerReturnsCreatedWhenRequestIsValid() throws Exception {
        
        // given: 테스트 준비.

        // authenticationService.register() 호출 시 성공 응답 DTO를 반환하도록 설정함.
        when(authenticationService.register(any(UserRegisterDto.Request.class)))
                .thenReturn(new UserRegisterDto.Response("회원가입이 완료되었습니다."));

        // when + then: 테스트 진행.

        // 가짜 HTTP POST 요청을 보냄
        // /api/users/register
        mockMvc.perform(post("/api/users/register")
                        // 요청 본문이 JSON 형식이라고 지정.
                        .contentType(MediaType.APPLICATION_JSON)
                        // 요청 body에 정상 회원가입 json을 넣음.
                            // 아래 정의된 validRegisterRequestJson() 사용.
                        .content(validRegisterRequestJson()))
                // 검증 시작
                // 응답 HTTP 상태 코드가 201 Created인지 확인.
                .andExpect(status().isCreated())
                // 응답 JSON의 message 필드 값이 기대값인지 확인.
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

        // 컨트롤러가 실제로 authenticationService.register()를 호출했는지 확인.
        // 요청이 컨트롤러에서 서비스로 정상 전달되었는지 확인하는 부분.
        verify(authenticationService).register(any(UserRegisterDto.Request.class));
    }

    // 테스트2: DTO 검증 실패 테스트.
    // 잘못된 요청이 들어오면 400 Bad Request와 validation 에러 응답을 반환하는지 확인.
    @Test
    void registerReturnsBadRequestWhenRequestIsInvalid() throws Exception {

        // given: 별도의 mock 설정이 없음.

        // when + then
        // when: 가짜 HTTP POST 요청을 보냄.
        mockMvc.perform(post("/api/users/register")
                        // when: 요청 본문이 JSON 형식이라고 지정.
                        .contentType(MediaType.APPLICATION_JSON)
                        // when: 요청 body에 DTO 검증 조건을 여러 개 위반한 내용 작성.
                        .content("""
                                {
                                  "email": "wrong-email",
                                  "password": "123",
                                  "userName": "",
                                  "gender": null,
                                  "birthDate": "2999-01-01",
                                  "goalType": null
                                }
                                """))
                // then: HTTP 응답 코드가 400 BadRequest인지 확인.
                .andExpect(status().isBadRequest())
                // then: 응답 JSON의 code가 VALIDATION_ERROR인지 확인.
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                // 응답 JSON의 message가 기대값인지 확인.
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));
    }

    // 테스트3: 중복 이메일 예외 테스트.
    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {

        // given: 테스트 준비.
        
        // 중복 이메일 발생 상황 설정.
        // 컨트롤러가 authenticationService.register() 호출 시 DuplicateEmailException을 던지도록 설정함.
        when(authenticationService.register(any(UserRegisterDto.Request.class)))
                .thenThrow(new DuplicateEmailException());

        // when + then: 테스트 실행 + 검증.

        // when: 회원가입 API에 POST요청.
        mockMvc.perform(post("/api/users/register")
                        // when: 요청 본문이 JSON 형식이라고 지정.
                        .contentType(MediaType.APPLICATION_JSON)
                        // when: 요청 body에는 정상 형식의 회원가입 JSON 입력.
                            // 아래의 validRegisterRequestJson() 사용.
                        .content(validRegisterRequestJson()))
                // then: 응답 HTTP 상태 코드가 409 Conflict()인지 확인.
                .andExpect(status().isConflict())
                // then: 응답 JSON의 code가 DUPLICATE_EMAIL인지 확인.
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                // then: 응답 JSON의 message가 기대값인지 확인.
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    // 정상 요청 JSON 생성 메서드.
    private String validRegisterRequestJson() {
        return """
                {
                  "email": "test@example.com",
                  "password": "password123",
                  "userName": "홍길동",
                  "gender": "MALE",
                  "birthDate": "1995-01-01",
                  "goalType": "LOSE_WEIGHT"
                }
                """;
    }

    // 로그인 컨트롤러 테스트.

    // 테스트1: 로그인 성공.
    @Test
    void loginReturnsOkWhenRequestIsValid() throws Exception{
        // given: userService.login 호출 시 성공 응답 DTO 반환 상황 생성.
        when(authenticationService.login(any(UserLoginDto.Request.class)))
            .thenReturn(UserLoginDto.Response
                .builder()
                .userId(1L)
                .email("test@example.com")
                .userName("홍길동")
                .role(UserRole.USER)
                .accessToken("access-token")
                .build());
        
        // when + then
        // when: 로그인 API 요청.
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)    // when: 요청 본문이 JSON 형식이라고 지정.
                .content(validLoginRequestJson()))  // when: 요청 body에는 정상 JSON 입력.
            .andExpect(status().isOk()) // then: 응답 HTTP 상태 코드가 200 OK인지 확인.
            .andExpect(jsonPath("$.userId").value(1L))  // then: 응답 데이터의 값 확인.
            .andExpect(jsonPath("$.email").value("test@example.com")) 
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.accessToken").value("access-token"));
        
        // then: 컨트롤러가 실제로 서비스를 호출했는지 확인. 요청의 컨트롤러 -> 서비스 전달 확인 부분.
        verify(authenticationService).login(any(UserLoginDto.Request.class));
    }

    // 테스트2: 요청 DTO 검증 실패.
    @Test
    void loginReturnsBadRequestWhenRequestIsInvalid() throws Exception{
        // when + then
        // when: 로그인 API 요청.
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)    // when: 요청 본문 JSON 형식 지정.
                // when: 요청 body에 DTO 조건 위배 내용 작성.
                .content("""                               
                        {
                            "email": "wrong-email",
                            "password": ""
                        }
                        """))
            .andExpect(status().isBadRequest()) // then: 응답 HTTP 상태 코드 400 Bad Request 확인.
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))    // then: 응답 데이터 값 확인.
            .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));
    }

    // 테스트3: 로그인 실패.
    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        // given: 로그인 정보 불일치 상황 설정.
        when(authenticationService.login(any(UserLoginDto.Request.class)))
            .thenThrow(new InvalidLoginException());
        
        // when + then
        // when: 로그인 API 요청.
        mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)    // when: 요청 본문 JSON 형식 지정.
            .content(validLoginRequestJson()))  // when: 요청 body에 정상 요청 JSON 입력.
        .andExpect(status().isUnauthorized())   // then: 응답 HTTP 상태 코드 401 Unauthorized 확인.
        .andExpect(jsonPath("$.code").value("INVALID_LOGIN"))   // then: 응답 데이터 값 확인.
        .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."));
    }

    // 정상 요청 JSON 생성 메서드.
    private String validLoginRequestJson() {
        return """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;
    }


    // 계정 조회 컨트롤러 테스트.
    // 테스트 1: 
    @Test
    void findAccountReturnsOkWhenAuthenticated() throws Exception {
        // given: 
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        // given:
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, 
                null, 
                userDetails.getAuthorities());

        // given: @AuthenticationPrincipal이 읽을 인증 정보를 SecurityContext에 직접 등록.
        SecurityContextHolder.getContext().setAuthentication(auth);
                
        // given: 조회 성공 설정.
        when(accountService.findAccount(1L))
            .thenReturn(UserAccountFindDto.Response
                .builder()
                .userId(1L)
                .email("test@example.com")
                .userName("홍길동")
                .role(UserRole.USER)
                .build());
        
        // when + then
        mockMvc.perform(get("/api/users/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.role").value("USER"));
            
        
        // then
        verify(accountService).findAccount(1L);

        // then: 다른 테스트에 인증 정보가 남지 않도록 정리.
        SecurityContextHolder.clearContext();
    }

    // 계정 정보 수정 컨트롤러 테스트.

    // 테스트1: 인증된 사용자가 정상 요청 시 계정 정보 수정 성공.
    @Test
    void modifyAccountReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        // given: @AuthenticationPrincipal이 읽을 인증 정보를 SecurityContext에 직접 등록.
        SecurityContextHolder.getContext().setAuthentication(auth);

        // given: 수정 성공 설정.
        when(accountService.modifyAccount(eq(1L), any(UserAccountModifyDto.Request.class)))
            .thenReturn(UserAccountModifyDto.Response
                .builder()
                .userId(1L)
                .email("new@example.com")
                .userName("김철수")
                .role(UserRole.USER)
                .build());

        // when + then
        mockMvc.perform(patch("/api/users/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validModifyAccountRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.email").value("new@example.com"))
            .andExpect(jsonPath("$.userName").value("김철수"))
            .andExpect(jsonPath("$.role").value("USER"));

        // then
        verify(accountService).modifyAccount(eq(1L), any(UserAccountModifyDto.Request.class));

        // then: 다른 테스트에 인증 정보가 남지 않도록 정리.
        SecurityContextHolder.clearContext();
    }

    // 테스트2: 계정 수정 요청 DTO 검증 실패.
    @Test
    void modifyAccountReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        // when + then
        mockMvc.perform(patch("/api/users/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "wrong-email",
                            "userName": ""
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));

        SecurityContextHolder.clearContext();
    }

    // 정상 계정 수정 요청 JSON 생성 메서드.
    private String validModifyAccountRequestJson() {
        return """
                {
                    "email": "new@example.com",
                    "userName": "김철수"
                }
                """;
    }

    // 비밀번호 변경 컨트롤러 테스트
    // 테스트1: 비밀번호 변경 성공
    @Test
    void changePasswordReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(accountService.changePassword(eq(1L), any(PasswordChangeDto.Request.class))).thenReturn(new PasswordChangeDto.Response("비밀번호가 변경되었습니다."));

        // when + then
        mockMvc.perform(patch("/api/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPasswordChangeRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));

        verify(accountService).changePassword(eq(1L), any(PasswordChangeDto.Request.class));

        SecurityContextHolder.clearContext();
    }
    // 테스트2: 요청값 검증 실패
    @Test
    void changePasswordReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        // when + then
        mockMvc.perform(patch("/api/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "currentPassword": "", 
                            "newPassword": "123"
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));
        
        SecurityContextHolder.clearContext();
    }

    // 테스트3: 현재 비밀번호 불일치
    @Test
    void changePasswordReturnsBadRequestWhenCurrentPasswordDoesNotMatch() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(accountService.changePassword(eq(1L), any(PasswordChangeDto.Request.class)))
            .thenThrow(new InvalidPasswordException());

        // when + then
        mockMvc.perform(patch("/api/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPasswordChangeRequestJson()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
            .andExpect(jsonPath("$.message").value("현재 비밀번호가 일치하지 않습니다."));

        SecurityContextHolder.clearContext();
    }

    // 정상 비밀번호 변경 요청 JSON 생성 메서드
    private String validPasswordChangeRequestJson() {
        return """
                {
                    "currentPassword": "old-password", 
                    "newPassword": "new-password123"
                }
                """;
    }

    // 프로필 조회 컨트롤러 테스트.

    // 테스트1: 인증된 사용자가 프로필 조회 성공.
    @Test
    void findProfileReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(profileService.findProfile(1L))
            .thenReturn(UserProfileDto.Response.builder()
                .userId(1L)
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 1, 1))
                .goalType(GoalType.LOSE_WEIGHT)
                .build());

        // when + then
        mockMvc.perform(get("/api/users/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.birthDate").value("1995-01-01"))
            .andExpect(jsonPath("$.goalType").value("LOSE_WEIGHT"));

        verify(profileService).findProfile(1L);

        SecurityContextHolder.clearContext();
    }

    // 테스트2: 프로필이 없으면 404 응답.
    @Test
    void findProfileReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(profileService.findProfile(1L))
            .thenThrow(new UserProfileNotFoundException());

        // when + then
        mockMvc.perform(get("/api/users/profile"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_PROFILE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("사용자 프로필을 찾을 수 없습니다."));

        SecurityContextHolder.clearContext();
    }

    // 프로필 수정 컨트롤러 테스트.

    // 테스트1: 인증된 사용자가 정상 요청 시 프로필 수정 성공.
    @Test
    void modifyProfileReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(profileService.modifyProfile(eq(1L), any(UserProfileDto.Request.class)))
            .thenReturn(UserProfileDto.Response.builder()
                .userId(1L)
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1998, 5, 10))
                .goalType(GoalType.MAINTAIN_WEIGHT)
                .build());

        // when + then
        mockMvc.perform(patch("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validModifyProfileRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.gender").value("FEMALE"))
            .andExpect(jsonPath("$.birthDate").value("1998-05-10"))
            .andExpect(jsonPath("$.goalType").value("MAINTAIN_WEIGHT"));

        verify(profileService).modifyProfile(eq(1L), any(UserProfileDto.Request.class));

        SecurityContextHolder.clearContext();
    }

    // 테스트2: 프로필 수정 요청 DTO 검증 실패.
    @Test
    void modifyProfileReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        // when + then
        mockMvc.perform(patch("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "gender": null,
                            "birthDate": "2999-01-01",
                            "goalType": null
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));

        SecurityContextHolder.clearContext();
    }

    // 테스트3: 수정할 프로필이 없으면 404 응답.
    @Test
    void modifyProfileReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(profileService.modifyProfile(eq(1L), any(UserProfileDto.Request.class)))
            .thenThrow(new UserProfileNotFoundException());

        // when + then
        mockMvc.perform(patch("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validModifyProfileRequestJson()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_PROFILE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("사용자 프로필을 찾을 수 없습니다."));

        SecurityContextHolder.clearContext();
    }

    // 정상 프로필 수정 요청 JSON 생성 메서드.
    private String validModifyProfileRequestJson() {
        return """
                {
                    "gender": "FEMALE",
                    "birthDate": "1998-05-10",
                    "goalType": "MAINTAIN_WEIGHT"
                }
                """;
    }

    // 신체 측정값 저장 컨트롤러 테스트.

    // 테스트1: 인증된 사용자가 정상 요청 시 신체 측정값 저장 성공.
    @Test
    void addBodyMeasurementReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(bodyMeasurementService.addBodyMeasurement(eq(1L), any(BodyMeasurementDto.Request.class)))
            .thenReturn(BodyMeasurementDto.Response.builder()
                .measurementId(10L)
                .measuredDate(LocalDate.of(2026, 7, 1))
                .height(175.5)
                .weight(70.2)
                .build());

        // when + then
        mockMvc.perform(post("/api/users/body-measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBodyMeasurementRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.measurementId").value(10L))
            .andExpect(jsonPath("$.measuredDate").value("2026-07-01"))
            .andExpect(jsonPath("$.height").value(175.5))
            .andExpect(jsonPath("$.weight").value(70.2));

        verify(bodyMeasurementService).addBodyMeasurement(eq(1L), any(BodyMeasurementDto.Request.class));

        SecurityContextHolder.clearContext();
    }

    // 테스트2: 신체 측정값 저장 요청 DTO 검증 실패.
    @Test
    void addBodyMeasurementReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        // when + then
        mockMvc.perform(post("/api/users/body-measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "measuredDate": "2999-01-01",
                            "height": 0,
                            "weight": null
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."));

        SecurityContextHolder.clearContext();
    }

    // 테스트3: 사용자가 존재하지 않으면 404 응답.
    @Test
    void addBodyMeasurementReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(bodyMeasurementService.addBodyMeasurement(eq(1L), any(BodyMeasurementDto.Request.class)))
            .thenThrow(new UserNotFoundException());

        // when + then
        mockMvc.perform(post("/api/users/body-measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBodyMeasurementRequestJson()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

        SecurityContextHolder.clearContext();
    }

    // 정상 신체 측정값 저장 요청 JSON 생성 메서드.
    private String validBodyMeasurementRequestJson() {
        return """
                {
                    "measuredDate": "2026-07-01",
                    "height": 175.5,
                    "weight": 70.2
                }
                """;
    }

    // 최신 신체 측정값 조회 컨트롤러 테스트.

    // 테스트1: 인증된 사용자가 최신 신체 측정값 조회 성공.
    @Test
    void findLatestBodyMeasurementReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(bodyMeasurementService.findLatestBodyMeasurement(1L))
            .thenReturn(BodyMeasurementDto.Response.builder()
                .measurementId(10L)
                .measuredDate(LocalDate.of(2026, 7, 2))
                .height(175.5)
                .weight(70.0)
                .build());

        // when + then
        mockMvc.perform(get("/api/users/body-measurements/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.measurementId").value(10L))
            .andExpect(jsonPath("$.measuredDate").value("2026-07-02"))
            .andExpect(jsonPath("$.height").value(175.5))
            .andExpect(jsonPath("$.weight").value(70.0));

        verify(bodyMeasurementService).findLatestBodyMeasurement(1L);

        SecurityContextHolder.clearContext();
    }

    // 테스트2: 최신 신체 측정값이 없으면 404 응답.
    @Test
    void findLatestBodyMeasurementReturnsNotFoundWhenMeasurementDoesNotExist() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(bodyMeasurementService.findLatestBodyMeasurement(1L))
            .thenThrow(new BodyMeasurementNotFoundException());

        // when + then
        mockMvc.perform(get("/api/users/body-measurements/latest"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("BODY_MEASUREMENT_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("신체 측정값을 찾을 수 없습니다."));

        SecurityContextHolder.clearContext();
    }

    // 전체 신체 측정 이력 조회 컨트롤러 테스트.

    // 테스트1: 인증된 사용자가 전체 신체 측정 이력 조회 성공.
    @Test
    void findBodyMeasurementsReturnsOkWhenAuthenticated() throws Exception {
        // given
        CustomUserDetails userDetails = createCustomUserDetails(1L);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        when(bodyMeasurementService.findBodyMeasurements(1L))
            .thenReturn(List.of(
                BodyMeasurementDto.Response.builder()
                    .measurementId(11L)
                    .measuredDate(LocalDate.of(2026, 7, 2))
                    .height(175.5)
                    .weight(70.0)
                    .build(),
                BodyMeasurementDto.Response.builder()
                    .measurementId(10L)
                    .measuredDate(LocalDate.of(2026, 7, 1))
                    .height(175.5)
                    .weight(70.2)
                    .build()));

        // when + then
        mockMvc.perform(get("/api/users/body-measurements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].measurementId").value(11L))
            .andExpect(jsonPath("$[0].measuredDate").value("2026-07-02"))
            .andExpect(jsonPath("$[0].height").value(175.5))
            .andExpect(jsonPath("$[0].weight").value(70.0))
            .andExpect(jsonPath("$[1].measurementId").value(10L))
            .andExpect(jsonPath("$[1].measuredDate").value("2026-07-01"))
            .andExpect(jsonPath("$[1].height").value(175.5))
            .andExpect(jsonPath("$[1].weight").value(70.2));

        verify(bodyMeasurementService).findBodyMeasurements(1L);

        SecurityContextHolder.clearContext();
    }


    // CustomDetails 생성 헬퍼 메서드.
    private CustomUserDetails createCustomUserDetails(Long userId) {
        User user = User
            .builder()
            .email("test@example.com")
            .passwordHash("hashed-password")
            .userName("홍길동")
            .build();
        
        ReflectionTestUtils.setField(user, "id", userId);

        return new CustomUserDetails(user);
    }

    

}
