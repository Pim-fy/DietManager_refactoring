package com.hwanghj.dietmanager.refactoring.backend.measurement;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

public class MeasurementConflictException extends BusinessException {
    public MeasurementConflictException(String message) { super(ErrorCode.BODY_MEASUREMENT_CONFLICT, message); }
}
