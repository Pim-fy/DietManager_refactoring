package com.hwanghj.dietmanager.refactoring.backend.domain.plan;
import java.time.LocalDate;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface MealPlanRepository extends JpaRepository<MealPlan,Long> {
    List<MealPlan> findByUserIdAndPlannedDateOrderByIdAsc(Long userId,LocalDate date);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MealPlan> findByIdAndUserId(Long id,Long userId);
}
