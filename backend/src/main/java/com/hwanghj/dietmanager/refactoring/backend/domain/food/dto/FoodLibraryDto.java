package com.hwanghj.dietmanager.refactoring.backend.domain.food.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class FoodLibraryDto {

    /**
     * 즐겨찾기 설정·해제 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FavoriteRequest {

        @NotBlank
        @Size(max = 100)
        private String foodCode;

        @NotNull
        private Boolean favorite;
    }

    /**
     * 저장 식품(즐겨찾기·최근 식품) 응답 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Item {
        private FoodNutrition food;
        private boolean favorite;
        private LocalDateTime lastUsedAt;
    }
}
