package com.hwanghj.dietmanager.refactoring.backend.domain.food.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class FoodApiLimitException extends BusinessException {
    public FoodApiLimitException() { super(ErrorCode.FOOD_API_LIMIT); }
}
