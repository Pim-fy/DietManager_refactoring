package com.hwanghj.dietmanager.refactoring.backend.exception;

public class UserProfileNotFoundException extends BusinessException {
    
    public UserProfileNotFoundException() {
        super(ErrorCode.USER_PROFILE_NOT_FOUND);
    }
}
