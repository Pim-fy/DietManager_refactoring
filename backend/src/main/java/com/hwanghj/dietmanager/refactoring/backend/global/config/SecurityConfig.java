package com.hwanghj.dietmanager.refactoring.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorResponse;
import com.hwanghj.dietmanager.refactoring.backend.global.security.JwtAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(ErrorCode.UNAUTHORIZED)));
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/foods/**", "/api/intakes/**", "/api/nutrition/**", "/api/statistics/**", "/api/meal-plans/**").authenticated()
                        .requestMatchers("/api/users/register", "/api/users/login", "/api/users/refresh", "/api/users/logout", "/error").permitAll()
                        .requestMatchers(
                                "/api/users/account",
                                "/api/users/password",
                                "/api/users/profile",
                                "/api/users/body-measurements/**")
                        .authenticated() // 토큰 필요
                        .anyRequest().authenticated() // 새 경로는 기본적으로 인증 필요
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
