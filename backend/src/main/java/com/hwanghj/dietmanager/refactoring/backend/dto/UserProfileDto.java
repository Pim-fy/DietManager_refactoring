package com.hwanghj.dietmanager.refactoring.backend.dto;

import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.common.Gender;
import com.hwanghj.dietmanager.refactoring.backend.common.GoalType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 조회,수정 요청/응답 DTO
 */
public class UserProfileDto {

    /**
     * 사용자 프로필 수정 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {

        @NotNull(message = "성별은 필수입니다.")
        private Gender gender;

        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        @NotNull(message = "생년월일은 필수입니다.")
        private LocalDate birthDate;

        @NotNull(message = "목표 유형은 필수입니다.")
        private GoalType goalType;
    }

    /**
     * 사용자 프로필 응답 DTO
     */
    @Getter
    public static class Response {

        private final Long userId;
        private final Gender gender;
        private final LocalDate birthDate;
        private final GoalType goalType;

        public Response(Long userId, Gender gender, LocalDate birthDate, GoalType goalType) {
            this.userId = userId;
            this.gender = gender;
            this.birthDate = birthDate;
            this.goalType = goalType;
        }
    }
}
