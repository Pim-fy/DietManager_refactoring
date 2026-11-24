package com.hwanghj.dietmanager.refactoring.backend.domain.user.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class InvalidLoginException extends BusinessException {
    
    public InvalidLoginException() {
        super(ErrorCode.INVALID_LOGIN);
    }
}
