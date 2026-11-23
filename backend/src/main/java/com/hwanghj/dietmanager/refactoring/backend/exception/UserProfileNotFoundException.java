package com.hwanghj.dietmanager.refactoring.backend.exception;

public class UserProfileNotFoundException extends RuntimeException {
    
    public UserProfileNotFoundException() {
        super("사용자 프로필을 찾을 수 없습니다.");
    }
}
