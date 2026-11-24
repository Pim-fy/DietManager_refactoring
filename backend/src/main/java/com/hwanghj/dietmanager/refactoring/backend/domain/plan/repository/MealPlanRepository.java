package com.hwanghj.dietmanager.refactoring.backend.domain.plan.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.hwanghj.dietmanager.refactoring.backend.domain.plan.entity.MealPlan;

import jakarta.persistence.LockModeType;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByUserIdAndPlannedDateOrderByIdAsc(Long userId, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MealPlan> findByIdAndUserId(Long id, Long userId);
}
