package com.hwanghj.dietmanager.refactoring.backend.dto;

import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.common.Gender;
import com.hwanghj.dietmanager.refactoring.backend.common.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.common.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청/응답 DTO
 */
public class UserSignUpDto {

    /**
     * 회원가입 요청 DTO        <br>
     * 전달 데이터의 유효성 검사
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해야 합니다.")
        private String password;

        @NotBlank(message = "사용자 이름은 필수입니다.")
        @Size(max = 30, message = "사용자 이름은 30자 이하로 입력해야 합니다.")
        private String userName;

        @NotNull(message = "성별은 필수입니다.")
        private Gender gender;

        // Past: 날짜가 과거인지 검사
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        @NotNull(message = "생년월일은 필수입니다.")
        private LocalDate birthDate;

        @NotNull(message = "목표 유형은 필수입니다.")
        private GoalType goalType;

        // PastOrPresent: 날짜가 과거이거나 오늘인지 검사
        @PastOrPresent(message = "측정일은 오늘 또는 과거 날짜여야 합니다.")
        @NotNull(message = "측정일은 필수입니다.")
        private LocalDate measuredDate;

        // Positive: 숫자가 0보다 큰 양수인지 검사
        @Positive(message = "키는 0보다 커야 합니다.")
        @NotNull(message = "키는 필수입니다.")
        private Double height;

        @Positive(message = "몸무게는 0보다 커야 합니다.")
        @NotNull(message = "몸무게는 필수입니다.")
        private Double weight;
    }

    /**
     * 회원가입 응답 DTO
     */
    @Getter
    public static class Response {

        private final Long userId;
        private final String email;
        private final String userName;
        private final UserRole role;

        public Response(Long userId, String email, String userName, UserRole role) {
            this.userId = userId;
            this.email = email;
            this.userName = userName;
            this.role = role;
        }
    }
}
