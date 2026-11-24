package com.hwanghj.dietmanager.refactoring.backend.domain.food.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodNutritionService;
@RestController
@RequestMapping("/api/foods")
@Validated
public class FoodController {
    private final FoodNutritionService client;
    public FoodController(FoodNutritionService client) { this.client = client; }
    @GetMapping
    public FoodNutrition.SearchResult search(@RequestParam @NotBlank @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return client.search(keyword.trim(), page, size);
    }
}

