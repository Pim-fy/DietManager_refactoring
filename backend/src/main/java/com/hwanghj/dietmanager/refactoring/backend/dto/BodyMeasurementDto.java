package com.hwanghj.dietmanager.refactoring.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신체 측정값 저장,조회 요청/응답 DTO
 */
public class BodyMeasurementDto {

    /**
     * 신체 측정값 저장 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {

        @PastOrPresent(message = "측정일은 오늘 또는 과거 날짜여야 합니다.")
        @NotNull(message = "측정일은 필수입니다.")
        private LocalDate measuredDate;

        @Positive(message = "키는 0보다 커야 합니다.")
        @NotNull(message = "키는 필수입니다.")
        private Double height;

        @Positive(message = "몸무게는 0보다 커야 합니다.")
        @NotNull(message = "몸무게는 필수입니다.")
        private Double weight;
    }

    /**
     * 신체 측정값 응답 DTO
     */
    @Getter
    public static class Response {

        private final Long measurementId;
        private final LocalDate measuredDate;
        private final Double height;
        private final Double weight;

        public Response(Long measurementId, LocalDate measuredDate, Double height, Double weight) {
            this.measurementId = measurementId;
            this.measuredDate = measuredDate;
            this.height = height;
            this.weight = weight;
        }
    }
}
