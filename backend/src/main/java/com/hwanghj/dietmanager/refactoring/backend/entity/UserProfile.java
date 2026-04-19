package com.hwanghj.dietmanager.refactoring.backend.entity;

import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.common.BaseTimeEntity;
import com.hwanghj.dietmanager.refactoring.backend.common.Gender;
import com.hwanghj.dietmanager.refactoring.backend.common.GoalType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권장 섭취량 계산에 필요한 고정성 프로필 담당
 */
@Entity
@Getter
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false)
    private GoalType goalType;

    public UserProfile(Gender gender, LocalDate birthDate, GoalType goalType) {
        this.gender = gender;
        this.birthDate = birthDate;
        this.goalType = goalType;
    }

    void setUser(User user) {
        this.user = user;
    }

    public void updateProfile(Gender gender, LocalDate birthDate, GoalType goalType) {
        this.gender = gender;
        this.birthDate = birthDate;
        this.goalType = goalType;
    }

}
