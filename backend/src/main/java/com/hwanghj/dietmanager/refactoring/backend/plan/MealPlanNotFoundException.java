package com.hwanghj.dietmanager.refactoring.backend.plan;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

public class MealPlanNotFoundException extends BusinessException {
    public MealPlanNotFoundException() { super(ErrorCode.MEAL_PLAN_NOT_FOUND); }
}
