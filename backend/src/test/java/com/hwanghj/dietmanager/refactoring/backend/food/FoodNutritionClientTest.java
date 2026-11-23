package com.hwanghj.dietmanager.refactoring.backend.food;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
class FoodNutritionClientTest {
    private final FoodNutritionClient client = new FoodNutritionClient("", new ObjectMapper());
    @Test void malformedSuccessIsNotAnEmptySearch() {
        assertThrows(FoodApiException.class, () -> client.parse("{\"header\":{\"resultCode\":\"00\"}}", 1, 10));
        assertThrows(FoodApiException.class, () -> client.parse("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"totalCount\":5,\"items\":{\"unexpected\":true}}}", 1, 10));
    }
    @Test void parsesArrayAndScalesByExplicitBasis() {
        var result = client.parse("""
            {"header":{"resultCode":"00"},"body":{"totalCount":1,"items":[{
            "FOOD_CD":"food1","FOOD_NM_KR":"밥","SERVING_SIZE":"100g",
            "AMT_NUM1":"1,234.5","AMT_NUM3":"2.2","AMT_NUM4":"-","AMT_NUM6":"N/A"}]}}
            """, 1, 10);
        var food = result.items().getFirst();
        assertEquals(new BigDecimal("100"), food.baseAmount());
        assertEquals("g", food.baseUnit());
        assertEquals(new BigDecimal("1234.5"), food.calories());
        assertNull(food.fat()); assertNull(food.carbohydrate()); assertTrue(food.recordable());
    }
    @Test void supportsWrappedSingletonAndMl() {
        var result = client.parse("""
            {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{
            "FOOD_CD":"drink","FOOD_NM_KR":"음료","SERVING_SIZE":"200 mL","AMT_NUM1":"0"}},"totalCount":"1"}}}
            """, 1, 10);
        assertEquals("ml", result.items().getFirst().baseUnit());
        assertEquals(BigDecimal.ZERO, result.items().getFirst().calories());
    }
    @Test void emptySearchIsAnEmptyList() {
        assertTrue(client.parse("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":[],\"totalCount\":0}}", 1, 10).items().isEmpty());
    }
    @Test void unsupportedBasisCannotBeRecorded() {
        var result = client.parse("""
            {"header":{"resultCode":"00"},"body":{"items":[{
            "FOOD_CD":"f","FOOD_NM_KR":"음식","SERVING_SIZE":"1개"}]}}
            """, 1, 10);
        assertFalse(result.items().getFirst().recordable());
    }
    @Test void remoteErrorDoesNotExposeRemoteMessageOrKey() {
        var error = assertThrows(FoodApiException.class, () -> client.parse("{\"header\":{\"resultCode\":\"30\",\"resultMsg\":\"secret-key-url\"}}", 1, 10));
        assertFalse(error.getMessage().contains("secret-key-url"));
        assertNull(error.getCause());
    }
}
