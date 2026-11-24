package com.hwanghj.dietmanager.refactoring.backend.domain.profile.exception;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

public class UserProfileNotFoundException extends BusinessException {
    
    public UserProfileNotFoundException() {
        super(ErrorCode.USER_PROFILE_NOT_FOUND);
    }
}
