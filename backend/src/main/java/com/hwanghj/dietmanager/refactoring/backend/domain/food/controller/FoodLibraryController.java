package com.hwanghj.dietmanager.refactoring.backend.domain.food.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodLibraryDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodLibraryService;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/foods")
public class FoodLibraryController {
    private final FoodLibraryService service;

    public FoodLibraryController(FoodLibraryService service) {
        this.service = service;
    }

    @GetMapping("/favorites")
    public List<FoodLibraryDto.Item> favorites(@AuthenticationPrincipal CustomUserDetails user) {
        return service.favorites(user.getUserId());
    }

    @GetMapping("/recent")
    public List<FoodLibraryDto.Item> recent(@AuthenticationPrincipal CustomUserDetails user) {
        return service.recent(user.getUserId());
    }

    @PutMapping("/favorites")
    public FoodLibraryDto.Item favorite(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody FoodLibraryDto.FavoriteRequest request) {
        return service.favorite(user.getUserId(), request.getFoodCode(), request.getFavorite());
    }
}
