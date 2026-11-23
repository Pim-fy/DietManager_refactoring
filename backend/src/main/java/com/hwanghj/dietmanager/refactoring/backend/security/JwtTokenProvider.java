package com.hwanghj.dietmanager.refactoring.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hwanghj.dietmanager.refactoring.backend.entity.User;

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

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;


    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs) {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            this.accessTokenValidityMs = accessTokenValidityMs;
    }

    // 로그인 성공 시 토큰 발급.
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts
            .builder()
            .subject(String.valueOf(user.getId()))
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact();
    }

    // 요청으로 들어온 토큰이 유효한지 확인.
    public boolean validateToken(String token) {
        try{
            Jwts.parser()
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
        String subject = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
        
        return Long.valueOf(subject);
    }

    
}
