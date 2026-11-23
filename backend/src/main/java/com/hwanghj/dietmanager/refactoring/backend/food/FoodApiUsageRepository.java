package com.hwanghj.dietmanager.refactoring.backend.food;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FoodApiUsageRepository extends JpaRepository<FoodApiUsage,LocalDate> {}
