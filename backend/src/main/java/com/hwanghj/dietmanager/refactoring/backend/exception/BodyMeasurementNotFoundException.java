package com.hwanghj.dietmanager.refactoring.backend.exception;

public class BodyMeasurementNotFoundException extends BusinessException {
    
    public BodyMeasurementNotFoundException() {
        super(ErrorCode.BODY_MEASUREMENT_NOT_FOUND);
    }
}
