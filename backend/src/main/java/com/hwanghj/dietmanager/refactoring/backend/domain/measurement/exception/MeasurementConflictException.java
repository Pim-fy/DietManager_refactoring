package com.hwanghj.dietmanager.refactoring.backend.domain.measurement.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class MeasurementConflictException extends BusinessException {
    public MeasurementConflictException(String message) { super(ErrorCode.BODY_MEASUREMENT_CONFLICT, message); }
}
