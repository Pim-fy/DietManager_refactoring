package com.hwanghj.dietmanager.refactoring.backend.domain.food.repository;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.entity.FoodApiUsage;
public interface FoodApiUsageRepository extends JpaRepository<FoodApiUsage,LocalDate> {}
