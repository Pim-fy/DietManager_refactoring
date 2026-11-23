package com.hwanghj.dietmanager.refactoring.backend.intake;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IntakeRepository extends JpaRepository<IntakeRecord, Long> {
    List<IntakeRecord> findByUserIdAndEatenDateAndDeletedFalseOrderByIdAsc(Long userId, LocalDate date);
    List<IntakeRecord> findByUserIdAndEatenDateAndDeletedTrueOrderByIdAsc(Long userId, LocalDate date);
    Optional<IntakeRecord> findByIdAndUserId(Long id, Long userId);
    List<IntakeRecord> findByUserIdAndDeletedFalseAndEatenDateBetweenOrderByEatenDateAscIdAsc(Long userId, LocalDate start, LocalDate end);
}
