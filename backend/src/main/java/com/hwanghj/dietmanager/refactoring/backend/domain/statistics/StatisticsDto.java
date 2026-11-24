package com.hwanghj.dietmanager.refactoring.backend.domain.statistics;
import java.time.LocalDate;
import java.util.List;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.IntakeDto;
public final class StatisticsDto {
    public record Day(LocalDate date, int records, IntakeDto.Total calories, IntakeDto.Total carbohydrate, IntakeDto.Total protein, IntakeDto.Total fat, Double weight) {}
    public record Result(LocalDate start, LocalDate end, List<Day> days) {}
}
