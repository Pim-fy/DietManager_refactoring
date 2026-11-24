package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.dto;

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
     * 하루 영양 합계와 체중
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
