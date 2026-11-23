package com.hwanghj.dietmanager.refactoring.backend.dto;

import com.hwanghj.dietmanager.refactoring.backend.common.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserAccountModifyDto {
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @NotBlank(message = "사용자 이름은 필수입니다.")
        @Size(max = 30, message = "사용자 이름은 30자 이하로 입력해야 합니다.")
        private String userName;
    }

    @Getter
    @Builder
    public static class Response {
        private final Long userId;
        private final String email;
        private final String userName;
        private final UserRole role;
    }


}
