package com.hwanghj.dietmanager.refactoring.backend.food;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hwanghj.dietmanager.refactoring.backend.security.CustomUserDetails;
@RestController @RequestMapping("/api/foods")
public class FoodLibraryController {
    private final FoodLibraryService service;
    public FoodLibraryController(FoodLibraryService service) { this.service=service; }
    public record FavoriteRequest(@NotBlank @Size(max=100) String foodCode,@NotNull Boolean favorite) {}
    @GetMapping("/favorites") public List<FoodLibraryService.Item> favorites(@AuthenticationPrincipal CustomUserDetails user) { return service.favorites(user.getUserId()); }
    @GetMapping("/recent") public List<FoodLibraryService.Item> recent(@AuthenticationPrincipal CustomUserDetails user) { return service.recent(user.getUserId()); }
    @PutMapping("/favorites") public FoodLibraryService.Item favorite(@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody FavoriteRequest request) { return service.favorite(user.getUserId(),request.foodCode(),request.favorite()); }
}
