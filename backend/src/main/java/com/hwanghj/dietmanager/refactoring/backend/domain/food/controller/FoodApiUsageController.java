package com.hwanghj.dietmanager.refactoring.backend.domain.food.controller;
import org.springframework.web.bind.annotation.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodApiQuota;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodApiUsageDto;
@RestController
public class FoodApiUsageController {
    private final FoodApiQuota quota;
    public FoodApiUsageController(FoodApiQuota quota) { this.quota=quota; }
    @GetMapping("/api/foods/usage") public FoodApiUsageDto.Status usage() { return quota.status(); }
}
