package com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity;

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
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @Column(length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AuthSession session;

    @Column(nullable = false)
    private boolean used;

    public RefreshToken(String hash, AuthSession session) {
        this.tokenHash = hash;
        this.session = session;
    }

    public void consume() {
        used = true;
    }
}
