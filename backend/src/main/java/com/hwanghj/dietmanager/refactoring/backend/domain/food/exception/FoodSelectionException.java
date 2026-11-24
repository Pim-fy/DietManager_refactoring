package com.hwanghj.dietmanager.refactoring.backend.domain.food.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class FoodSelectionException extends BusinessException {
    public FoodSelectionException(String message) {
        super(ErrorCode.FOOD_SELECTION_CONFLICT, message);
    }
}
