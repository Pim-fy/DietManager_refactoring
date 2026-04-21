package com.hwanghj.dietmanager.refactoring.backend.entity;

import com.hwanghj.dietmanager.refactoring.backend.common.BaseTimeEntity;
import com.hwanghj.dietmanager.refactoring.backend.common.UserRole;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인, 계정 정보 담당.
 */
@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "user_name", nullable = false, unique = true, length = 30)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // cascade: User를 저장, 삭제 시 연결된 UserProfile도 함께 저장, 삭제.
    // orphanRemoval: User와 연결이 끊긴 UserProfile을 고아 객체로 보고 삭제.
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    public User(String email, String passwordHash, String userName) {
        this.email = email;
        updatePassword(passwordHash);
        this.userName = userName;
        this.role = UserRole.USER;
    }

    public void updateAccount(String email, String userName) {
        this.email = email;
        this.userName = userName;
    }

    public void updatePassword(String passwordHash) {
        if(passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("비밀번호 해시는 비어 있을 수 없습니다.");
        }

        this.passwordHash = passwordHash;
    }

    public void changeRole(UserRole role) {
        if(role == null) {
            throw new IllegalArgumentException("권한은 null일 수 없습니다.");
        }
        this.role = role;
    }

    public void assignProfile(UserProfile profile) {
        if(profile == null) {
            throw new IllegalArgumentException("프로필은 null일 수 없습니다.");
        }
        if(this.profile != null) {
            throw new IllegalStateException("이미 프로필이 존재합니다.");
        }
        this.profile = profile;
        profile.setUser(this);
    }

    
}
