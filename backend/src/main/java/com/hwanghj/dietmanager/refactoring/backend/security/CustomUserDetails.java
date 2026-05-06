package com.hwanghj.dietmanager.refactoring.backend.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hwanghj.dietmanager.refactoring.backend.entity.User;

/**
 * 토큰 인증이 끝난 뒤, 컨트롤러에서 현재 로그인한 사용자를 꺼내기 위해 필요한 객체.
 */
public class CustomUserDetails implements UserDetails{
    private final Long userId;
    private final String email;
    private final String password;
    private final String role;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.role = user.getRole().name();
    }

    // 현재 로그인한 사용자의 ID를 얻음.
    public Long getUserId() {
        return userId;
    }

    // 현재 로그인한 사용자의 권한.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    // 현재 로그인한 사용자의 비밀번호 해시값.
    @Override
    public String getPassword() {
        return password;
    }

    // 현재 로그인한 사용자를 식별하는 로그인 식별자.
    @Override
    public String getUsername() {
        return email;
    }
}
