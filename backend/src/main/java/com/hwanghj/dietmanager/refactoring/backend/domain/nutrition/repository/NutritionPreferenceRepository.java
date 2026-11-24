package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.NutritionPreference;

public interface NutritionPreferenceRepository extends JpaRepository<NutritionPreference, Long> {
}
