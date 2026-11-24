package com.hwanghj.dietmanager.refactoring.backend.domain.food.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodLibraryDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.entity.SavedFood;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodSelectionException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.repository.SavedFoodRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

@Service
public class FoodLibraryService {
    private final Clock clock;
    private final SavedFoodRepository repository;
    private final FoodNutritionService source;
    private final UserRepository users;

    public FoodLibraryService(SavedFoodRepository repository, FoodNutritionService source, UserRepository users) {
        this(repository, source, users, Clock.systemDefaultZone());
    }

    @Autowired
    public FoodLibraryService(SavedFoodRepository repository, FoodNutritionService source, UserRepository users, Clock clock) {
        this.clock = clock;
        this.repository = repository;
        this.source = source;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<FoodLibraryDto.Item> favorites(Long userId) {
        return repository.findByUserIdAndFavoriteTrueOrderByIdDesc(userId, PageRequest.of(0, 100)).stream().map(this::item).toList();
    }

    @Transactional(readOnly = true)
    public List<FoodLibraryDto.Item> recent(Long userId) {
        return repository.findByUserIdAndLastUsedAtIsNotNullOrderByLastUsedAtDescIdDesc(userId, PageRequest.of(0, 20)).stream().map(this::item).toList();
    }

    @Transactional(readOnly = true)
    public FoodNutrition resolve(Long userId, String code) {
        try {
            return source.selectedFood(code);
        } catch (FoodSelectionException failure) {
            return repository.findByUserIdAndSnapshotFoodCode(userId, code).map(saved -> saved.getSnapshot().food()).orElseThrow(() -> failure);
        }
    }

    @Transactional
    public FoodLibraryDto.Item favorite(Long userId, String code, boolean value) {
        var existing = repository.findByUserIdAndSnapshotFoodCode(userId, code);
        if (!value && existing.isEmpty())
            throw new FoodNotFoundException();
        if (value && (existing.isEmpty() || !existing.get().isFavorite()) && repository.countByUserIdAndFavoriteTrue(userId) >= 100)
            throw new FoodSelectionException("즐겨찾기는 최대 100개까지 저장할 수 있습니다.");
        var saved = existing.orElseGet(() -> new SavedFood(users.findById(userId).orElseThrow(UserNotFoundException::new), resolve(userId, code)));
        saved.favorite(value);
        return item(repository.save(saved));
    }

    @Transactional
    public void used(Long userId, FoodNutrition food) {
        var saved = repository.findByUserIdAndSnapshotFoodCode(userId, food.getFoodCode())
                .orElseGet(() -> new SavedFood(users.findById(userId).orElseThrow(UserNotFoundException::new), food));
        saved.used(food, LocalDateTime.now(clock));
        repository.save(saved);
    }

    private FoodLibraryDto.Item item(SavedFood saved) {
        return FoodLibraryDto.Item.builder()
                .food(saved.getSnapshot().food())
                .favorite(saved.isFavorite())
                .lastUsedAt(saved.getLastUsedAt())
                .build();
    }
}
