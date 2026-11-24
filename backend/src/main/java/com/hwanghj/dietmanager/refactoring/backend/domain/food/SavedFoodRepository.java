package com.hwanghj.dietmanager.refactoring.backend.domain.food;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SavedFoodRepository extends JpaRepository<SavedFood,Long> {
    long countByUserIdAndFavoriteTrue(Long userId);
    Optional<SavedFood> findByUserIdAndSnapshotFoodCode(Long userId,String code);
    List<SavedFood> findByUserIdAndFavoriteTrueOrderByIdDesc(Long userId, Pageable page);
    List<SavedFood> findByUserIdAndLastUsedAtIsNotNullOrderByLastUsedAtDescIdDesc(Long userId, Pageable page);
}
