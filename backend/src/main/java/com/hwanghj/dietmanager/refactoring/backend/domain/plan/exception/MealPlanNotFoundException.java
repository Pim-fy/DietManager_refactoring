package com.hwanghj.dietmanager.refactoring.backend.domain.plan.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class MealPlanNotFoundException extends BusinessException {
    public MealPlanNotFoundException() { super(ErrorCode.MEAL_PLAN_NOT_FOUND); }
}
