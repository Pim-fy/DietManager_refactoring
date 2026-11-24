package com.hwanghj.dietmanager.refactoring.backend.domain.statistics.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class StatisticsRangeException extends BusinessException {
    public StatisticsRangeException(String message) { super(ErrorCode.VALIDATION_ERROR, message); }
}
