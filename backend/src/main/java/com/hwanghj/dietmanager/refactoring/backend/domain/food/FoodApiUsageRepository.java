package com.hwanghj.dietmanager.refactoring.backend.domain.food;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FoodApiUsageRepository extends JpaRepository<FoodApiUsage,LocalDate> {}
