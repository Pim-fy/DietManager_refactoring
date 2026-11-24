package com.hwanghj.dietmanager.refactoring.backend.domain.food.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodNutritionService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/foods")
@Validated
public class FoodController {
    private final FoodNutritionService client;

    public FoodController(FoodNutritionService client) {
        this.client = client;
    }

    @GetMapping
    public FoodNutrition.SearchResult search(@RequestParam @NotBlank(message = "검색어는 필수입니다.") @Size(max = 100, message = "검색어는 100자 이하여야 합니다.") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지는 1 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.") int size) {
        return client.search(keyword.trim(), page, size);
    }
}
