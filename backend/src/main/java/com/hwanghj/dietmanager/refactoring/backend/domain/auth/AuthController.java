package com.hwanghj.dietmanager.refactoring.backend.domain.auth;

import java.net.URI;
import java.util.Map;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.exception.OriginNotAllowedException;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.exception.SessionExpiredException;
@RestController
@RequestMapping("/api/users")
public class AuthController {
    private final AuthSessionService sessions;
    public AuthController(AuthSessionService sessions) { this.sessions = sessions; }
    @PostMapping("/refresh")
    public Map<String,String> refresh(@CookieValue(name=AuthCookies.NAME, required=false) String token,
            HttpServletRequest request, HttpServletResponse response) {
        checkOrigin(request);
        try {
            var pair=sessions.refresh(token);
            AuthCookies.write(response,pair.refreshToken());
            return Map.of("accessToken",pair.accessToken());
        } catch (SessionExpiredException error) {
            AuthCookies.write(response,null); throw error;
        }
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name=AuthCookies.NAME, required=false) String token,
            HttpServletRequest request, HttpServletResponse response) {
        checkOrigin(request); sessions.logout(token); AuthCookies.write(response,null);
    }
    private void checkOrigin(HttpServletRequest request) {
        String origin=request.getHeader("Origin");
        if ("cross-site".equals(request.getHeader("Sec-Fetch-Site"))) throw new OriginNotAllowedException();
        if (origin==null) return; // 비브라우저 클라이언트는 쿠키를 명시적으로 제공한다.
        try {
            URI uri=URI.create(origin);
            boolean loopback="localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
            if (!"http".equals(uri.getScheme()) || !loopback ||
                    (uri.getPort()!=5186 && uri.getPort()!=request.getServerPort())) throw new IllegalArgumentException();
        } catch (IllegalArgumentException error) { throw new OriginNotAllowedException(); }
    }
}
