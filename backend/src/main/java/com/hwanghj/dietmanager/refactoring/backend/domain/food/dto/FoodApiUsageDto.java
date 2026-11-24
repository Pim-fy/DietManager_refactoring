package com.hwanghj.dietmanager.refactoring.backend.domain.food.dto;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class FoodApiUsageDto {

    /**
     * 식품 API 호출량·한도 응답 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Status {
        private LocalDate date;
        private int calls;
        private int dayLimit;
        private int recentMinuteCalls;
        private int minuteLimit;
    }
}
