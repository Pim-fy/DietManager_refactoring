package com.hwanghj.dietmanager.refactoring.backend.domain.intake.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class IntakeNotFoundException extends BusinessException {
    public IntakeNotFoundException() { super(ErrorCode.INTAKE_NOT_FOUND); }
}
