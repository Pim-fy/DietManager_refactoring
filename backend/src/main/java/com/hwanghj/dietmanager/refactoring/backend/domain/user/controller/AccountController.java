package com.hwanghj.dietmanager.refactoring.backend.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.PasswordChangeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.AccountService;
@RestController
@RequestMapping("/api/users")
public class AccountController {
    private final AccountService accountService;
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/account")
    public ResponseEntity<UserAccountFindDto.Response> findAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountService.findAccount(userDetails.getUserId()));
    }

    @PatchMapping("/account")
    public ResponseEntity<UserAccountModifyDto.Response> modifyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserAccountModifyDto.Request request) {
        return ResponseEntity.ok(
            accountService.modifyAccount(userDetails.getUserId(), request));
    }

    @PatchMapping("/password")
    public ResponseEntity<PasswordChangeDto.Response> changePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody PasswordChangeDto.Request request) {
            return ResponseEntity.ok(accountService.changePassword(userDetails.getUserId(), request));
    }
}
