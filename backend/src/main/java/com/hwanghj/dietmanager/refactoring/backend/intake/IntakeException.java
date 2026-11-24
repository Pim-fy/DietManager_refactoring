package com.hwanghj.dietmanager.refactoring.backend.intake;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class IntakeException extends BusinessException {
    public IntakeException(String message) { super(ErrorCode.INTAKE_CONFLICT, message); }
}
