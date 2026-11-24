package com.hwanghj.dietmanager.refactoring.backend.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserLoginDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.dto.UserRegisterDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.service.AuthenticationService;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthenticationService authenticationService;
    private final com.hwanghj.dietmanager.refactoring.backend.domain.auth.service.LoginAttemptLimiter loginLimiter;
    public UserController(AuthenticationService authenticationService, com.hwanghj.dietmanager.refactoring.backend.domain.auth.service.LoginAttemptLimiter loginLimiter) {
        this.authenticationService = authenticationService;
        this.loginLimiter = loginLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegisterDto.Response> register(
        @Valid @RequestBody UserRegisterDto.Request request) {
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginDto.Response> login(
        @Valid @RequestBody UserLoginDto.Request request, jakarta.servlet.http.HttpServletResponse servletResponse,
        jakarta.servlet.http.HttpServletRequest servletRequest) {
        loginLimiter.check(servletRequest.getRemoteAddr(),request.getEmail());
        UserLoginDto.Response result;
        try { result = authenticationService.login(request); }
        catch (com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.InvalidLoginException error) {
            loginLimiter.failed(servletRequest.getRemoteAddr(),request.getEmail()); throw error;
        }
        loginLimiter.succeeded(request.getEmail());
        if (result.getRefreshToken() != null) com.hwanghj.dietmanager.refactoring.backend.domain.auth.controller.AuthCookies.write(servletResponse, result.getRefreshToken());
        return ResponseEntity
            .ok(result);
    }
}
