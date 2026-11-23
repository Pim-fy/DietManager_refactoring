package com.hwanghj.dietmanager.refactoring.backend.food;
public class FoodApiLimitException extends RuntimeException {
    public FoodApiLimitException() { super("식품 API 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요."); }
}
