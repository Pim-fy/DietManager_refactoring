package com.hwanghj.dietmanager.refactoring.backend.domain.food;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodSelectionException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.client.FoodNutritionClient;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodNutritionService;
class FoodCacheClockTest {
    @Test void cacheExpiresAtBoundaryWithoutSleeping() {
        var client = mock(FoodNutritionClient.class);
        var clock = mock(Clock.class);
        var start = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var food = FoodNutrition.builder().foodCode("CLOCK").foodName("clock").makerName("").servingSize("100g").baseAmount(BigDecimal.valueOf(100)).baseUnit("g").calories(BigDecimal.ONE).sourceUpdatedDate("").build();
        when(client.search("clock", 1, 10)).thenReturn(FoodNutrition.SearchResult.builder().items(List.of(food)).page(1).size(10).totalCount(1).build());
        var service = new FoodNutritionService(client, clock);
        service.search("clock", 1, 10);
        when(clock.instant()).thenReturn(start.plusSeconds(299));
        service.search("clock", 1, 10);
        verify(client, times(1)).search("clock", 1, 10);
        when(clock.instant()).thenReturn(start.plusSeconds(300));
        service.search("clock", 1, 10);
        verify(client, times(2)).search("clock", 1, 10);
        when(clock.instant()).thenReturn(start.plusSeconds(3900));
        assertThatThrownBy(() -> service.selectedFood("CLOCK")).isInstanceOf(FoodSelectionException.class);
    }
}
