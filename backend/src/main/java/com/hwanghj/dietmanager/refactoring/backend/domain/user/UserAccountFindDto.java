package com.hwanghj.dietmanager.refactoring.backend.domain.user;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.UserRole;

import lombok.Builder;
import lombok.Getter;

public class UserAccountFindDto {
    @Getter
    @Builder
    public static class Response {
        private final Long userId;
        private final String email;
        private final String userName;
        private final UserRole role;
    }
    
}
