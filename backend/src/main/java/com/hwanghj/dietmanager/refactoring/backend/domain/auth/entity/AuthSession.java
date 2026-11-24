package com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_sessions")
@Getter
@NoArgsConstructor
public class AuthSession {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long tokenVersion;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    public AuthSession(User user) {
        this(user, LocalDateTime.now(Clock.systemUTC()));
    }

    public AuthSession(User user, LocalDateTime now) {
        id = UUID.randomUUID().toString();
        this.user = user;
        tokenVersion = user.getTokenVersion();
        expiresAt = now.plusDays(14);
    }

    public void revoke() {
        revoked = true;
    }
}
