package com.hwanghj.dietmanager.refactoring.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hwanghj.dietmanager.refactoring.backend.dto.UserAccountFindDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserAccountModifyDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.security.CustomUserDetails;
import com.hwanghj.dietmanager.refactoring.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegisterDto.Response> register(
        @Valid @RequestBody UserRegisterDto.Request request) {
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginDto.Response> login(
        @Valid @RequestBody UserLoginDto.Request request) {
        return ResponseEntity
            .ok(userService.login(request));            // 로그인 성공 시 200 OK와 response 반환.
    }

    @GetMapping("/account")
    public ResponseEntity<UserAccountFindDto.Response> findAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.findAccount(userDetails.getUserId()));
    }

    @PatchMapping("/account")
    public ResponseEntity<UserAccountModifyDto.Response> modifyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserAccountModifyDto.Request request) {
        return ResponseEntity.ok(
            userService.modifyAccount(userDetails.getUserId(), request));
    }
}
