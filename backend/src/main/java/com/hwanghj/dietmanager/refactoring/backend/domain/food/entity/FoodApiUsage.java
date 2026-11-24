package com.hwanghj.dietmanager.refactoring.backend.domain.food.entity;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="food_api_usage") @Getter @NoArgsConstructor
public class FoodApiUsage {
    @Id @Column(name="usage_date") private LocalDate date;
    @Column(nullable=false) private int calls;
    public FoodApiUsage(LocalDate date) { this.date=date; }
    public void increment() { calls++; }
}
