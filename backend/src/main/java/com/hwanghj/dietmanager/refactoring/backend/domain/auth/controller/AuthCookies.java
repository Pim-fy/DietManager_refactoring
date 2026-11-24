package com.hwanghj.dietmanager.refactoring.backend.domain.auth.controller;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;

public final class AuthCookies {
    public static final String NAME = "dietmanager_refresh";
    private AuthCookies() {}
    public static void write(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", ResponseCookie.from(NAME, token == null ? "" : token)
            .httpOnly(true).sameSite("Strict").path("/api/users")
            .maxAge(token == null ? 0 : 14 * 24 * 60 * 60).build().toString());
        response.setHeader("Cache-Control", "no-store");
    }
}
