package com.hwanghj.dietmanager.refactoring.backend.food;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

public class FoodNotFoundException extends BusinessException {
    public FoodNotFoundException() { super(ErrorCode.FOOD_NOT_FOUND); }
}
