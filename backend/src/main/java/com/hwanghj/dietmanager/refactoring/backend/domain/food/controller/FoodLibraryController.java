package com.hwanghj.dietmanager.refactoring.backend.domain.food.controller;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodLibraryService;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodLibraryDto;
@RestController @RequestMapping("/api/foods")
public class FoodLibraryController {
    private final FoodLibraryService service;
    public FoodLibraryController(FoodLibraryService service) { this.service=service; }
    @GetMapping("/favorites") public List<FoodLibraryDto.Item> favorites(@AuthenticationPrincipal CustomUserDetails user) { return service.favorites(user.getUserId()); }
    @GetMapping("/recent") public List<FoodLibraryDto.Item> recent(@AuthenticationPrincipal CustomUserDetails user) { return service.recent(user.getUserId()); }
    @PutMapping("/favorites") public FoodLibraryDto.Item favorite(@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody FoodLibraryDto.FavoriteRequest request) { return service.favorite(user.getUserId(),request.getFoodCode(),request.getFavorite()); }
}
