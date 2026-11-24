package com.hwanghj.dietmanager.refactoring.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hwanghj.dietmanager.refactoring.backend.user.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component  // JstTokenProvider를 Spring Bean으로 만듦.
public class JwtTokenProvider {
    /*
        createAccessToken(User user)
        validateToken(String token)
        getUserId(String token)
    */

    private final java.time.Clock clock;
    private final SecretKey secretKey;
    private final long accessTokenValidityMs;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs, java.time.Clock clock) {
            this.clock = clock;
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            this.accessTokenValidityMs = accessTokenValidityMs;
    }

    public JwtTokenProvider(String secret, long accessTokenValidityMs) {
        this(secret, accessTokenValidityMs, java.time.Clock.systemUTC());
    }

    // 로그인 성공 시 토큰 발급.
    public String createAccessToken(User user) {
        return createAccessToken(user, null);
    }
    public String createAccessToken(User user, String sessionId) {
        Date now = Date.from(clock.instant());
        Date expiration = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts
            .builder()
            .subject(String.valueOf(user.getId()))
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim("version", user.getTokenVersion())
            .claim("sid", sessionId)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact();
    }

    // 요청으로 들어온 토큰이 유효한지 확인.
    public boolean validateToken(String token) {
        try{
            Jwts.parser().clock(() -> Date.from(clock.instant()))
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch(JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 사용자 ID 추출.
    public Long getUserId(String token) {
        String subject = Jwts.parser().clock(() -> Date.from(clock.instant()))
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
        
        return Long.valueOf(subject);
    }

    /** 서명·만료·subject를 한 번에 검증해 필터 밖으로 토큰 오류가 전파되지 않게 한다. */
    public Long authenticatedUserId(String token) {
        try {
            Long id = getUserId(token);
            return id > 0 ? id : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public record Authentication(Long userId, long version, String sessionId) {}
    public Authentication authentication(String token) {
        try {
            var claims = Jwts.parser().clock(() -> Date.from(clock.instant())).verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            Long id = Long.valueOf(claims.getSubject());
            if (id <= 0 || claims.getExpiration() == null) return null;
            Long version = claims.get("version", Long.class);
            return new Authentication(id, version == null ? 0 : version, claims.get("sid", String.class));
        } catch (JwtException | IllegalArgumentException e) { return null; }
    }

    
}
