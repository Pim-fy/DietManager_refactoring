package com.hwanghj.dietmanager.refactoring.backend.entity;

import java.time.LocalDate;

import com.hwanghj.dietmanager.refactoring.backend.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 키, 몸무게 측정 이력 담당
 */
@Entity
@Getter
@Table(name = "body_measurements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyMeasurement extends BaseTimeEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "measured_date", nullable = false)
    private LocalDate measuredDate;

    @Column(nullable = false)
    private double height;

    @Column(nullable = false)
    private double weight;

    public BodyMeasurement(User user, LocalDate measuredDate, double height, double weight) {
        this.user = user;
        this.measuredDate = measuredDate;
        this.height = height;
        this.weight = weight;
    }

}
