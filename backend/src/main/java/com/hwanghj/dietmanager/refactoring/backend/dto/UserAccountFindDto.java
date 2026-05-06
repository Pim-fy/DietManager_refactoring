package com.hwanghj.dietmanager.refactoring.backend.dto;

import com.hwanghj.dietmanager.refactoring.backend.common.UserRole;

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
