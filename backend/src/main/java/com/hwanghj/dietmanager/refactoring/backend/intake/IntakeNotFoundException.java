package com.hwanghj.dietmanager.refactoring.backend.intake;
public class IntakeNotFoundException extends RuntimeException {
    public IntakeNotFoundException() { super("섭취 기록을 찾을 수 없습니다."); }
}
