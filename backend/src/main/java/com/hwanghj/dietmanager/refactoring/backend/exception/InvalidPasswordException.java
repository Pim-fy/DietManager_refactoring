package com.hwanghj.dietmanager.refactoring.backend.exception;

public class InvalidPasswordException extends BusinessException {
    
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }
}
