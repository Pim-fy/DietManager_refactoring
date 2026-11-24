package com.hwanghj.dietmanager.refactoring.backend.domain.food.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class FoodNotFoundException extends BusinessException {
    public FoodNotFoundException() { super(ErrorCode.FOOD_NOT_FOUND); }
}
