package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.IntakeRecord;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.repository.IntakeRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.service.IntakeMapper;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.service.NutritionService;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.dto.StatisticsDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.exception.StatisticsRangeException;

@Service
public class StatisticsService {
    private final IntakeRepository intakes;
    private final BodyMeasurementRepository measurements;
    private final NutritionService nutrition;

    public StatisticsService(IntakeRepository intakes, BodyMeasurementRepository measurements, NutritionService nutrition) {
        this.intakes = intakes;
        this.measurements = measurements;
        this.nutrition = nutrition;
    }

    @Transactional(readOnly = true)
    public StatisticsDto.Result range(Long userId, LocalDate start, LocalDate end) {
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 365)
            throw new StatisticsRangeException("날짜 범위는 최대 366일입니다.");
        var records = intakes.findByUserIdAndDeletedFalseAndEatenDateBetweenOrderByEatenDateAscIdAsc(userId, start, end);
        var grouped = records.stream().collect(Collectors.groupingBy(IntakeRecord::getEatenDate));
        Map<LocalDate, Double> weights = new HashMap<>();
        for (var record : measurements.findByUserIdAndDeletedFalseAndMeasuredDateBetweenOrderByMeasuredDateAscIdAsc(userId, start, end))
            weights.put(record.getMeasuredDate(), record.getWeight());
        // 권장량 계산 재료는 기간 끝 기준으로 한 번만 조회하고 날짜별 계산은 메모리에서 한다.
        var timeline = nutrition.timeline(userId, end);
        List<StatisticsDto.Day> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            var items = grouped.getOrDefault(date, List.of());
            days.add(StatisticsDto.Day.builder()
                    .date(date)
                    .records(items.size())
                    .calories(IntakeMapper.aggregate(items, IntakeRecord::effectiveCalories))
                    .carbohydrate(IntakeMapper.aggregate(items, IntakeRecord::effectiveCarbohydrate))
                    .protein(IntakeMapper.aggregate(items, IntakeRecord::effectiveProtein))
                    .fat(IntakeMapper.aggregate(items, IntakeRecord::effectiveFat))
                    .weight(weights.get(date))
                    .recommendedCalories(timeline.at(date).getCalories())
                    .build());
        }
        return StatisticsDto.Result.builder()
                .start(start)
                .end(end)
                .days(days)
                .build();
    }
}
