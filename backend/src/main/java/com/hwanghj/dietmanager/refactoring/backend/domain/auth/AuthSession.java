package com.hwanghj.dietmanager.refactoring.backend.domain.auth;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.User;
@Entity @Table(name="auth_sessions") @Getter @NoArgsConstructor
public class AuthSession {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(nullable=false) private long tokenVersion;
    @Column(nullable=false) private LocalDateTime expiresAt;
    @Column(nullable=false) private boolean revoked;
    public AuthSession(User user) { this(user, LocalDateTime.now(java.time.Clock.systemUTC())); }
    public AuthSession(User user, LocalDateTime now) { id=java.util.UUID.randomUUID().toString(); this.user=user; tokenVersion=user.getTokenVersion(); expiresAt=now.plusDays(14); }
    public void revoke() { revoked=true; }
}
