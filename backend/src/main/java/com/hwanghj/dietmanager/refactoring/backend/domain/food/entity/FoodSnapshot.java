package com.hwanghj.dietmanager.refactoring.backend.domain.food.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
/** 외부 검색 결과의 저장 가능한 영양 원본. 즐겨찾기와 식단 계획에서 함께 사용한다. */
@Embeddable @NoArgsConstructor
public class FoodSnapshot {
    @Column(name="food_code", nullable=false, length=100) private String foodCode;
    @Column(name="food_name", nullable=false, length=500) private String foodName;
    @Column(name="maker_name", length=500) private String makerName;
    @Column(name="serving_size", length=100) private String servingSize;
    @Column(name="base_amount", nullable=false, precision=20, scale=6) private BigDecimal baseAmount;
    @Column(name="base_unit", nullable=false, length=10) private String baseUnit;
    @Column(precision=20, scale=6) private BigDecimal calories;
    @Column(precision=20, scale=6) private BigDecimal carbohydrate;
    @Column(precision=20, scale=6) private BigDecimal protein;
    @Column(precision=20, scale=6) private BigDecimal fat;
    @Column(name="source_updated_date", length=50) private String sourceUpdatedDate;
    public FoodSnapshot(FoodNutrition food) {
        foodCode=food.foodCode(); foodName=food.foodName(); makerName=food.makerName(); servingSize=food.servingSize();
        baseAmount=food.baseAmount(); baseUnit=food.baseUnit(); calories=food.calories(); carbohydrate=food.carbohydrate();
        protein=food.protein(); fat=food.fat(); sourceUpdatedDate=food.sourceUpdatedDate();
    }
    public FoodNutrition food() { return new FoodNutrition(foodCode,foodName,makerName,servingSize,baseAmount,baseUnit,calories,carbohydrate,protein,fat,sourceUpdatedDate); }
}
