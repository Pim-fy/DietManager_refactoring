package com.hwanghj.dietmanager.refactoring.backend.domain.food.entity;

import java.time.LocalDateTime;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name="saved_foods", uniqueConstraints=@UniqueConstraint(name="uq_saved_user_food", columnNames={"user_id","food_code"}))
@Getter @NoArgsConstructor
public class SavedFood {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    @Embedded private FoodSnapshot snapshot;
    @Column(nullable=false) private boolean favorite;
    private LocalDateTime lastUsedAt;
    public SavedFood(User user, FoodNutrition food) { this.user=user; this.snapshot=new FoodSnapshot(food); }
    public void favorite(boolean value) { favorite=value; }
    public void used(FoodNutrition food) { used(food, LocalDateTime.now()); }
    public void used(FoodNutrition food, LocalDateTime now) { snapshot=new FoodSnapshot(food); lastUsedAt=now; }
}
