package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hwanghj.dietmanager.refactoring.backend.domain.intake.dto.IntakeDto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class StatisticsDto {

    /**
     * 하루 영양 합계, 체중, 권장 칼로리
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Day {
        private LocalDate date;
        private int records;
        private IntakeDto.Total calories;
        private IntakeDto.Total carbohydrate;
        private IntakeDto.Total protein;
        private IntakeDto.Total fat;
        private Double weight;
        // 그날의 권장 칼로리(권장량 API의 calories와 같은 값). 계산할 수 없으면 null
        private BigDecimal recommendedCalories;
    }

    /**
     * 기간 통계 응답 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Result {
        private LocalDate start;
        private LocalDate end;
        private List<Day> days;
    }
}
