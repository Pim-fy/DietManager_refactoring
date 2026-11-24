package com.hwanghj.dietmanager.refactoring.backend.domain.plan.entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.hwanghj.dietmanager.refactoring.backend.global.common.BaseTimeEntity;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.entity.FoodSnapshot;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.MealType;
@Entity @Table(name="meal_plans",indexes=@Index(name="idx_plan_user_date",columnList="user_id,planned_date"))
@Getter @NoArgsConstructor
public class MealPlan extends BaseTimeEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    @Embedded private FoodSnapshot snapshot;
    @Column(nullable=false) private LocalDate plannedDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private MealType mealType;
    @Column(nullable=false,precision=20,scale=6) private BigDecimal amount;
    @Column(nullable=false) private boolean deleted;
    private Long intakeId;
    public MealPlan(User user,FoodNutrition food,LocalDate date,MealType meal,BigDecimal amount) { this.user=user; snapshot=new FoodSnapshot(food); update(date,meal,amount); }
    public void update(LocalDate date,MealType meal,BigDecimal amount) { plannedDate=date; mealType=meal; this.amount=amount; }
    public void delete() { deleted=true; }
    public void restore() { deleted=false; }
    public void consumed(Long id) { intakeId=id; }
}
