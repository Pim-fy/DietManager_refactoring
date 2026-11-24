package com.hwanghj.dietmanager.refactoring.backend.plan;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class MealPlanConflictException extends BusinessException {
    public MealPlanConflictException(String message) { super(ErrorCode.MEAL_PLAN_CONFLICT, message); }
}
