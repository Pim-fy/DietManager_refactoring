package com.hwanghj.dietmanager.refactoring.backend.domain.auth;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity @Table(name="refresh_tokens") @Getter @NoArgsConstructor
public class RefreshToken {
    @Id @Column(length=64) private String tokenHash;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private AuthSession session;
    @Column(nullable=false) private boolean used;
    public RefreshToken(String hash,AuthSession session) { this.tokenHash=hash; this.session=session; }
    public void consume() { used=true; }
}
