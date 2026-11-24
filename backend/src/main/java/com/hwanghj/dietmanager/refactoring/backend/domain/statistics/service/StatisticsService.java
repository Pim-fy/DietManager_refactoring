package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import com.hwanghj.dietmanager.refactoring.backend.domain.measurement.repository.BodyMeasurementRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.service.IntakeMapper;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.IntakeRecord;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.repository.IntakeRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.exception.StatisticsRangeException;
import com.hwanghj.dietmanager.refactoring.backend.domain.statistics.dto.StatisticsDto;
@org.springframework.stereotype.Service
public class StatisticsService {
    private final IntakeRepository intakes;
    private final BodyMeasurementRepository measurements;
    public StatisticsService(IntakeRepository intakes, BodyMeasurementRepository measurements) { this.intakes = intakes; this.measurements = measurements; }
    @Transactional(readOnly = true)
    public StatisticsDto.Result range(Long userId, LocalDate start, LocalDate end) {
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 365) throw new StatisticsRangeException( "날짜 범위는 최대 366일입니다.");
        var records = intakes.findByUserIdAndDeletedFalseAndEatenDateBetweenOrderByEatenDateAscIdAsc(userId, start, end);
        var grouped = records.stream().collect(java.util.stream.Collectors.groupingBy(IntakeRecord::getEatenDate));
        Map<LocalDate, Double> weights = new HashMap<>();
        for (var record : measurements.findByUserIdAndDeletedFalseAndMeasuredDateBetweenOrderByMeasuredDateAscIdAsc(userId, start, end)) weights.put(record.getMeasuredDate(), record.getWeight());
        List<StatisticsDto.Day> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            var items = grouped.getOrDefault(date, List.of());
            days.add(new StatisticsDto.Day(date, items.size(), IntakeMapper.aggregate(items, IntakeRecord::effectiveCalories), IntakeMapper.aggregate(items, IntakeRecord::effectiveCarbohydrate), IntakeMapper.aggregate(items, IntakeRecord::effectiveProtein), IntakeMapper.aggregate(items, IntakeRecord::effectiveFat), weights.get(date)));
        }
        return new StatisticsDto.Result(start, end, days);
    }
}
