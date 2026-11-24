package com.hwanghj.dietmanager.refactoring.backend.domain.food.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodSelectionException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.client.FoodNutritionClient;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
/** 제한된 메모리 캐시로 검색 호출량을 줄이고, 섭취 저장에 신뢰할 수 있는 원본 값을 제공한다. */
@Service
public class FoodNutritionService {
    private record Cached<T>(T value, Instant expires) {}
    private final java.time.Clock clock;
    private final FoodNutritionClient client;
    private final Map<String, Cached<FoodNutrition.SearchResult>> searches = new LinkedHashMap<>();
    private final Map<String, Cached<FoodNutrition>> foods = new LinkedHashMap<>();
    public FoodNutritionService(FoodNutritionClient client) { this(client, java.time.Clock.systemDefaultZone()); }
    @org.springframework.beans.factory.annotation.Autowired
    public FoodNutritionService(FoodNutritionClient client, java.time.Clock clock) { this.clock = clock; this.client = client;  }

    public FoodNutrition.SearchResult search(String keyword, int page, int size) {
        String key = keyword + ":" + page + ":" + size;
        synchronized (this) {
            var hit = searches.get(key);
            if (hit != null && hit.expires().isAfter(Instant.now(clock))) {
                rememberFoods(hit.value());
                return hit.value();
            }
        }
        var result = client.search(keyword, page, size);
        synchronized (this) {
            searches.put(key, new Cached<>(result, Instant.now(clock).plusSeconds(300)));
            rememberFoods(result);
            trim(searches, 100);
        }
        return result;
    }
    private void rememberFoods(FoodNutrition.SearchResult result) {
        for (var food : result.items()) foods.put(food.foodCode(), new Cached<>(food, Instant.now(clock).plusSeconds(3600)));
        trim(foods, 1000);
    }
    public synchronized FoodNutrition selectedFood(String code) {
        var hit = foods.get(code);
        if (hit == null || !hit.expires().isAfter(Instant.now(clock))) {
            throw new FoodSelectionException("선택한 식품 정보가 만료되었습니다. 식품을 다시 검색해주세요.");
        }
        if (!hit.value().recordable()) throw new FoodSelectionException("기준량 단위를 확인할 수 없는 식품은 섭취 기록에 추가할 수 없습니다.");
        return hit.value();
    }
    private static void trim(Map<?, ?> map, int max) {
        while (map.size() > max) { var iterator = map.keySet().iterator(); iterator.next(); iterator.remove(); }
    }
}
