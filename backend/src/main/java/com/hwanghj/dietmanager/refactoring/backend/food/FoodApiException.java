package com.hwanghj.dietmanager.refactoring.backend.food;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

public class FoodApiException extends BusinessException {
    public FoodApiException(String message) { super(ErrorCode.FOOD_API_ERROR, message); }
}
