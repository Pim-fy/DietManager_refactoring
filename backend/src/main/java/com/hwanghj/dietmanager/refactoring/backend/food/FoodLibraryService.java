package com.hwanghj.dietmanager.refactoring.backend.food;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import com.hwanghj.dietmanager.refactoring.backend.user.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.exception.UserNotFoundException;

@Service
public class FoodLibraryService {
    private final java.time.Clock clock;
    private final SavedFoodRepository repository;
    private final FoodNutritionService source;
    private final UserRepository users;
    public FoodLibraryService(SavedFoodRepository repository,FoodNutritionService source,UserRepository users) { this(repository, source, users, java.time.Clock.systemDefaultZone()); }
    @org.springframework.beans.factory.annotation.Autowired
    public FoodLibraryService(SavedFoodRepository repository,FoodNutritionService source,UserRepository users, java.time.Clock clock) { this.clock = clock; this.repository=repository; this.source=source; this.users=users;  }
    public record Item(FoodNutrition food,boolean favorite,java.time.LocalDateTime lastUsedAt) {}
    @Transactional(readOnly=true)
    public List<Item> favorites(Long userId) { return repository.findByUserIdAndFavoriteTrueOrderByIdDesc(userId,PageRequest.of(0,100)).stream().map(this::item).toList(); }
    @Transactional(readOnly=true)
    public List<Item> recent(Long userId) { return repository.findByUserIdAndLastUsedAtIsNotNullOrderByLastUsedAtDescIdDesc(userId,PageRequest.of(0,20)).stream().map(this::item).toList(); }
    @Transactional(readOnly=true)
    public FoodNutrition resolve(Long userId,String code) {
        try { return source.selectedFood(code); }
        catch (FoodSelectionException failure) {
            return repository.findByUserIdAndSnapshotFoodCode(userId,code).map(saved->saved.getSnapshot().food()).orElseThrow(()->failure);
        }
    }
    @Transactional
    public Item favorite(Long userId,String code,boolean value) {
        var existing=repository.findByUserIdAndSnapshotFoodCode(userId,code);
        if (!value && existing.isEmpty()) throw new FoodNotFoundException();
        if (value && (existing.isEmpty() || !existing.get().isFavorite()) && repository.countByUserIdAndFavoriteTrue(userId)>=100) throw new FoodSelectionException("즐겨찾기는 최대 100개까지 저장할 수 있습니다.");
        var saved=existing.orElseGet(()->new SavedFood(users.findById(userId).orElseThrow(UserNotFoundException::new),resolve(userId,code)));
        saved.favorite(value); return item(repository.save(saved));
    }
    @Transactional
    public void used(Long userId,FoodNutrition food) {
        var saved=repository.findByUserIdAndSnapshotFoodCode(userId,food.foodCode()).orElseGet(()->new SavedFood(users.findById(userId).orElseThrow(UserNotFoundException::new),food));
        saved.used(food, java.time.LocalDateTime.now(clock)); repository.save(saved);
    }
    private Item item(SavedFood saved) { return new Item(saved.getSnapshot().food(),saved.isFavorite(),saved.getLastUsedAt()); }
}
