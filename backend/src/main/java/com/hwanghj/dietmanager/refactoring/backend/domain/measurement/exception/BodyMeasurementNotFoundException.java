package com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class BodyMeasurementNotFoundException extends BusinessException {
    
    public BodyMeasurementNotFoundException() {
        super(ErrorCode.BODY_MEASUREMENT_NOT_FOUND);
    }
}
