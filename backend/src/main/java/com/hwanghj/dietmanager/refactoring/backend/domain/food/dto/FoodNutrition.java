package com.hwanghj.dietmanager.refactoring.backend.domain.food.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 외부 DB의 기준량과 영양값. 누락 영양값은 null로 유지한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FoodNutrition {
    private String foodCode;
    private String foodName;
    private String makerName;
    private String servingSize;
    private BigDecimal baseAmount;
    private String baseUnit;
    private BigDecimal calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;
    private String sourceUpdatedDate;

    /** 기준량과 단위를 알아서 섭취량 환산이 가능한지. JSON 응답에는 포함하지 않는다. */
    public boolean recordable() {
        return baseAmount != null && baseAmount.signum() > 0 && baseUnit != null;
    }

    /**
     * 식품 검색 결과 응답 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SearchResult {
        private List<FoodNutrition> items;
        private int page;
        private int size;
        private int totalCount;
    }
}
