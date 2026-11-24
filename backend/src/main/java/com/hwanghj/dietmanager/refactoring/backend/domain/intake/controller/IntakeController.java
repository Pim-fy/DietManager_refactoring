package com.hwanghj.dietmanager.refactoring.backend.domain.intake.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.domain.intake.dto.IntakeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.service.IntakeService;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/intakes")
public class IntakeController {
    private final IntakeService service;

    public IntakeController(IntakeService service) {
        this.service = service;
    }

    @GetMapping
    public IntakeDto.DailyResponse daily(@AuthenticationPrincipal CustomUserDetails user, @RequestParam LocalDate date) {
        return service.daily(user.getUserId(), date);
    }

    @PostMapping
    public ResponseEntity<IntakeDto.Response> create(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody IntakeDto.CreateRequest request) {
        return ResponseEntity.status(201).body(service.create(user.getUserId(), request));
    }

    @PostMapping("/manual")
    public ResponseEntity<IntakeDto.Response> manual(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody IntakeDto.ManualRequest request) {
        return ResponseEntity.status(201).body(service.createManual(user.getUserId(), request));
    }

    @PatchMapping("/{id}/nutrition")
    public IntakeDto.Response nutrition(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
            @Valid @RequestBody IntakeDto.NutritionInput request) {
        return service.editNutrition(user.getUserId(), id, request);
    }

    @PatchMapping("/{id}")
    public IntakeDto.Response update(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
            @Valid @RequestBody IntakeDto.UpdateRequest request) {
        return service.update(user.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        service.delete(user.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public IntakeDto.Response restore(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return service.restore(user.getUserId(), id);
    }
}
