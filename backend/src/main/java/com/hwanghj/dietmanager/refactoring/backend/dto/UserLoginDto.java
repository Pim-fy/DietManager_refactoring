package com.hwanghj.dietmanager.refactoring.backend.dto;

import com.hwanghj.dietmanager.refactoring.backend.common.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 
 * 로그인 요청/응답 DTO
 */
public class UserLoginDto {
    

    /**
     * 로그인 요청 DTO      <br>
     * 전달 데이터의 유효성 검사
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    public static class Request {

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    /**
     * 로그인 응답 DTO      <br>
     */
    @Getter
    @Builder
    public static class Response {

        private final Long userId;
        private final String email;
        private final String userName;
        private final UserRole role;
        private final String accessToken;
    }
}
