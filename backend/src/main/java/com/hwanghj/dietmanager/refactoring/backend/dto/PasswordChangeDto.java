package com.hwanghj.dietmanager.refactoring.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청/응답 DTO
 */
public class PasswordChangeDto {

    /**
     * 비밀번호 변경 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {

        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "새 비밀번호는 8자 이상 64자 이하로 입력해야 합니다.")
        private String newPassword;
    }

    /**
     * 비밀번호 변경 응답 DTO
     */
    @Getter
    public static class Response {

        private final String message;

        public Response(String message) {
            this.message = message;
        }
    }
}
