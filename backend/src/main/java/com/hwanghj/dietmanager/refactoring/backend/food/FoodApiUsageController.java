package com.hwanghj.dietmanager.refactoring.backend.food;
import org.springframework.web.bind.annotation.*;
@RestController
public class FoodApiUsageController {
    private final FoodApiQuota quota;
    public FoodApiUsageController(FoodApiQuota quota) { this.quota=quota; }
    @GetMapping("/api/foods/usage") public FoodApiQuota.Status usage() { return quota.status(); }
}
