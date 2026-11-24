package com.hwanghj.dietmanager.refactoring.backend.global.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.service.AuthSessionService;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 Authorization 헤더를 읽고, 토큰이 유효한 경우 Spring Security에 "인증된 사용자"를 등록.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuthSessionService sessions;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository, AuthSessionService sessions) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.sessions = sessions;
    }

    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolvetoken(request);
        
        var claims = token == null ? null : jwtTokenProvider.authentication(token);
        Long userId = claims == null ? null : claims.getUserId();
        if(userId != null) {

            User user = userRepository.findById(userId).orElse(null);
            
            if(user != null && user.getTokenVersion()==claims.getVersion() && sessions.active(claims.getSessionId(),userId,claims.getVersion())) {
                CustomUserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolvetoken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}

    
