package com.hwanghj.dietmanager.refactoring.backend.food;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

public class FoodSelectionException extends BusinessException {
    public FoodSelectionException(String message) { super(ErrorCode.FOOD_SELECTION_CONFLICT, message); }
}
