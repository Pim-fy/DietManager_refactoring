package com.hwanghj.dietmanager.refactoring.backend.exception;

public class InvalidLoginException extends BusinessException {
    
    public InvalidLoginException() {
        super(ErrorCode.INVALID_LOGIN);
    }
}
