package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.controller;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.dto.StatisticsDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.service.StatisticsService;
@RestController @RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService service;
    public StatisticsController(StatisticsService service) { this.service = service; }
    @GetMapping
    public StatisticsDto.Result range(@AuthenticationPrincipal CustomUserDetails user, @RequestParam LocalDate start, @RequestParam LocalDate end) {
        return service.range(user.getUserId(), start, end);
    }
}
