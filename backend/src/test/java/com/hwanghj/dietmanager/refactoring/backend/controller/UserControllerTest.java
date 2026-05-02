package com.hwanghj.dietmanager.refactoring.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hwanghj.dietmanager.refactoring.backend.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.exception.DuplicateEmailException;
import com.hwanghj.dietmanager.refactoring.backend.exception.GlobalExceptionHandler;
import com.hwanghj.dietmanager.refactoring.backend.service.UserService;

// 회원가입 컨트롤러 테스트 코드.
// 컨트롤러 테스트: HTTP응답 확인이 목적.
// Mockito 테스트를 쓰겠다는 설정.
    // Mockito: 가짜 객체(Mock)을 만들고 관리할 수 있게 해주는 테스트 프레임워크.
    // Mock 객체 생성 -> 행동 정의 -> 검증
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    // 컨트롤러가 호출할 서비스를 mock으로 둠.
    @Mock
    private UserService userService;

    // MockMvc: 실제 서버를 띄우지 않고도 HTTP 요청처럼 컨트롤러를 테스트하게 해주는 도구.
    private MockMvc mockMvc;

    // 각 테스트 메서드 실행 전 매번 실행.
    @BeforeEach
    void setUp() {
        // MockMvc 객체 생성.
        mockMvc = MockMvcBuilders
                // UserController만 단독 테스트 대상으로 등록.
                // 컨트롤러에 userService를 넣어줌.
                .standaloneSetup(new UserController(userService))
                // 테스트 중 예외 발생 시 실제처럼 GlobalExceptionHandler가 처리하게 등록함.
                    // DuplicateEmailException, MethodArgumentNotValidException 검증 위함.
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    
    // 테스트1: 정상 회원가입 요청 테스트.
    // 정상적인 회원가입 요청을 보내면 201 created와 함께 성공 메시지 반환하는지 확인.
    @Test
    void registerReturnsCreatedWhenRequestIsValid() throws Exception {
        
        // given: 테스트 준비.

        // userService.register() 호출 시 성공 응답 DTO를 반환하도록 설정함.
        when(userService.register(any(UserRegisterDto.Request.class)))
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

        // 컨트롤러가 실제로 userService.register()를 호출했는지 확인.
        // 요청이 컨트롤러에서 서비스로 정상 전달되었는지 확인하는 부분.
        verify(userService).register(any(UserRegisterDto.Request.class));
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
        // 컨트롤러가 userService.register() 호출 시 DuplicateEmailException을 던지도록 설정함.
        when(userService.register(any(UserRegisterDto.Request.class)))
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
}
