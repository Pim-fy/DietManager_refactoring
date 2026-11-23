package com.hwanghj.dietmanager.refactoring.backend.plan;
public class MealPlanNotFoundException extends RuntimeException {
    public MealPlanNotFoundException() { super("섭취 기록을 찾을 수 없습니다."); }
}
