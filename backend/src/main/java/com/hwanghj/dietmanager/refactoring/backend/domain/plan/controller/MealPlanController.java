package com.hwanghj.dietmanager.refactoring.backend.domain.plan.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.domain.plan.dto.MealPlanDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.plan.service.MealPlanService;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {
    private final MealPlanService service;

    public MealPlanController(MealPlanService service) {
        this.service = service;
    }

    @GetMapping
    public List<MealPlanDto.Response> daily(@AuthenticationPrincipal CustomUserDetails user, @RequestParam LocalDate date) {
        return service.daily(user.getUserId(), date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealPlanDto.Response create(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody MealPlanDto.Request request) {
        return service.create(user.getUserId(), request);
    }

    @PatchMapping("/{id}")
    public MealPlanDto.Response update(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
            @Valid @RequestBody MealPlanDto.Request request) {
        return service.update(user.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        service.delete(user.getUserId(), id);
    }

    @PostMapping("/{id}/restore")
    public MealPlanDto.Response restore(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return service.restore(user.getUserId(), id);
    }

    @PostMapping("/{id}/consume")
    public MealPlanDto.Response consume(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return service.consume(user.getUserId(), id);
    }
}
