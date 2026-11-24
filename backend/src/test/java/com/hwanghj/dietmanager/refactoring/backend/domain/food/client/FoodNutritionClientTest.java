package com.hwanghj.dietmanager.refactoring.backend.domain.food.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiException;

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
        var food = result.getItems().getFirst();
        assertEquals(new BigDecimal("100"), food.getBaseAmount());
        assertEquals("g", food.getBaseUnit());
        assertEquals(new BigDecimal("1234.5"), food.getCalories());
        assertNull(food.getFat()); assertNull(food.getCarbohydrate()); assertTrue(food.recordable());
    }
    @Test void supportsWrappedSingletonAndMl() {
        var result = client.parse("""
            {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{
            "FOOD_CD":"drink","FOOD_NM_KR":"음료","SERVING_SIZE":"200 mL","AMT_NUM1":"0"}},"totalCount":"1"}}}
            """, 1, 10);
        assertEquals("ml", result.getItems().getFirst().getBaseUnit());
        assertEquals(BigDecimal.ZERO, result.getItems().getFirst().getCalories());
    }
    @Test void emptySearchIsAnEmptyList() {
        assertTrue(client.parse("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":[],\"totalCount\":0}}", 1, 10).getItems().isEmpty());
    }
    @Test void unsupportedBasisCannotBeRecorded() {
        var result = client.parse("""
            {"header":{"resultCode":"00"},"body":{"items":[{
            "FOOD_CD":"f","FOOD_NM_KR":"음식","SERVING_SIZE":"1개"}]}}
            """, 1, 10);
        assertFalse(result.getItems().getFirst().recordable());
    }
    @Test void remoteErrorDoesNotExposeRemoteMessageOrKey() {
        var error = assertThrows(FoodApiException.class, () -> client.parse("{\"header\":{\"resultCode\":\"30\",\"resultMsg\":\"secret-key-url\"}}", 1, 10));
        assertFalse(error.getMessage().contains("secret-key-url"));
        assertNull(error.getCause());
    }
}
