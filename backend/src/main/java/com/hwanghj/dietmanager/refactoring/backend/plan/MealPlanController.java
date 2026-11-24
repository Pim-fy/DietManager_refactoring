package com.hwanghj.dietmanager.refactoring.backend.plan;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;
@RestController @RequestMapping("/api/meal-plans")
public class MealPlanController {
    private final MealPlanService service;
    public MealPlanController(MealPlanService service) { this.service = service; }
    @GetMapping
    public List<MealPlanDto.Response> daily(@AuthenticationPrincipal CustomUserDetails user, @RequestParam LocalDate date) { return service.daily(user.getUserId(), date); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public MealPlanDto.Response create(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody MealPlanDto.Request request) { return service.create(user.getUserId(), request); }
    @PatchMapping("/{id}")
    public MealPlanDto.Response update(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @Valid @RequestBody MealPlanDto.Request request) { return service.update(user.getUserId(), id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { service.delete(user.getUserId(), id); }
    @PostMapping("/{id}/restore")
    public MealPlanDto.Response restore(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { return service.restore(user.getUserId(), id); }
    @PostMapping("/{id}/consume")
    public MealPlanDto.Response consume(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { return service.consume(user.getUserId(), id); }
}
