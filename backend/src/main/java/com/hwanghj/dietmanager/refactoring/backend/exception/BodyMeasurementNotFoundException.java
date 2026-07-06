package com.hwanghj.dietmanager.refactoring.backend.exception;

public class BodyMeasurementNotFoundException extends RuntimeException {
    
    public BodyMeasurementNotFoundException() {
        super("신체 측정값을 찾을 수 없습니다.");
    }
}
