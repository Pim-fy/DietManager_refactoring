package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.dto.StatisticsDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.service.StatisticsService;
import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

@RestController @RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService service;
    public StatisticsController(StatisticsService service) { this.service = service; }
    @GetMapping
    public StatisticsDto.Result range(@AuthenticationPrincipal CustomUserDetails user, @RequestParam LocalDate start, @RequestParam LocalDate end) {
        return service.range(user.getUserId(), start, end);
    }
}
