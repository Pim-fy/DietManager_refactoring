package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.controller;
import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.dto.NutritionDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service.NutritionService;
@RestController @RequestMapping("/api/nutrition")
public class NutritionController {
    private final NutritionService service;
    public NutritionController(NutritionService service) { this.service = service; }
    public record PreferenceRequest(@NotNull ActivityLevel activityLevel) {}
    @GetMapping("/preferences") public NutritionDto.Preference preference(@AuthenticationPrincipal CustomUserDetails user) { return service.preference(user.getUserId()); }
    @PutMapping("/preferences") public NutritionDto.Preference save(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody PreferenceRequest request) { return service.save(user.getUserId(), request.activityLevel()); }
    @GetMapping("/recommendation") public NutritionDto.Recommendation recommendation(@AuthenticationPrincipal CustomUserDetails user, @RequestParam LocalDate date) { return service.recommendation(user.getUserId(), date); }
}
