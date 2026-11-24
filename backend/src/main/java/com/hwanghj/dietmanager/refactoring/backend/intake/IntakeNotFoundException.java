package com.hwanghj.dietmanager.refactoring.backend.intake;

import com.hwanghj.dietmanager.refactoring.backend.exception.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.exception.ErrorCode;

public class IntakeNotFoundException extends BusinessException {
    public IntakeNotFoundException() { super(ErrorCode.INTAKE_NOT_FOUND); }
}
