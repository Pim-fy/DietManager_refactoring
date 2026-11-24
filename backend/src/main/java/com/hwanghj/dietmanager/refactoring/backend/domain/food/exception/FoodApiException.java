package com.hwanghj.dietmanager.refactoring.backend.domain.food.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class FoodApiException extends BusinessException {
    public FoodApiException(String message) {
        super(ErrorCode.FOOD_API_ERROR, message);
    }
}
