package com.hwanghj.dietmanager.refactoring.backend.intake;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.hwanghj.dietmanager.refactoring.backend.global.common.BaseTimeEntity;
import com.hwanghj.dietmanager.refactoring.backend.user.User;
import com.hwanghj.dietmanager.refactoring.backend.food.FoodNutrition;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 영양값을 복사해 원본 DB 갱신이나 외부 API 장애에도 과거 기록을 유지한다. */
@Entity
@Table(name = "intake_records", indexes = @Index(name = "idx_intake_user_date", columnList = "user_id,eaten_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntakeRecord extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "eaten_date", nullable = false) private LocalDate eatenDate;
    @Enumerated(EnumType.STRING) @Column(name = "meal_type", nullable = false, length = 20) private MealType mealType;
    @Column(name = "food_code", nullable = false, length = 100) private String foodCode;
    @Column(name = "food_name", nullable = false, length = 500) private String foodName;
    @Column(name = "maker_name", length = 500) private String makerName;
    @Column(nullable = false, precision = 20, scale = 6) private BigDecimal amount;
    @Column(name = "base_amount", nullable = false, precision = 20, scale = 6) private BigDecimal baseAmount;
    @Column(nullable = false, length = 10) private String unit;
    @Column(name = "base_calories", precision = 20, scale = 6) private BigDecimal baseCalories;
    @Column(name = "base_carbohydrate", precision = 20, scale = 6) private BigDecimal baseCarbohydrate;
    @Column(name = "base_protein", precision = 20, scale = 6) private BigDecimal baseProtein;
    @Column(name = "base_fat", precision = 20, scale = 6) private BigDecimal baseFat;
    @Column(name = "source_updated_date", length = 50) private String sourceUpdatedDate;
    @Column(nullable = false) private boolean deleted;
    @Column(nullable=false) private boolean manual;
    @Column(nullable=false) private boolean nutritionEdited;
    @Column(length=500) private String editedFoodName;
    @Column(precision=20,scale=6) private BigDecimal editedBaseAmount;
    @Column(precision=20,scale=6) private BigDecimal editedCalories;
    @Column(precision=20,scale=6) private BigDecimal editedCarbohydrate;
    @Column(precision=20,scale=6) private BigDecimal editedProtein;
    @Column(precision=20,scale=6) private BigDecimal editedFat;

    public void markManual() { manual=true; }
    public void editNutrition(IntakeDto.NutritionInput input) {
        nutritionEdited=true; editedFoodName=input.foodName().trim(); editedBaseAmount=input.baseAmount();
        editedCalories=input.calories(); editedCarbohydrate=input.carbohydrate(); editedProtein=input.protein(); editedFat=input.fat();
    }
    public String effectiveFoodName() { return nutritionEdited ? editedFoodName : foodName; }
    public BigDecimal effectiveBaseAmount() { return nutritionEdited ? editedBaseAmount : baseAmount; }
    public BigDecimal effectiveCalories() { return nutritionEdited ? editedCalories : baseCalories; }
    public BigDecimal effectiveCarbohydrate() { return nutritionEdited ? editedCarbohydrate : baseCarbohydrate; }
    public BigDecimal effectiveProtein() { return nutritionEdited ? editedProtein : baseProtein; }
    public BigDecimal effectiveFat() { return nutritionEdited ? editedFat : baseFat; }

    public IntakeRecord(User user, FoodNutrition food, LocalDate date, MealType meal, BigDecimal amount) {
        this.user = user; this.foodCode = food.foodCode(); this.foodName = food.foodName(); this.makerName = food.makerName();
        this.baseAmount = food.baseAmount(); this.unit = food.baseUnit(); this.baseCalories = food.calories();
        this.baseCarbohydrate = food.carbohydrate(); this.baseProtein = food.protein(); this.baseFat = food.fat();
        this.sourceUpdatedDate = food.sourceUpdatedDate(); update(date, meal, amount);
    }
    public void update(LocalDate date, MealType meal, BigDecimal amount) { this.eatenDate = date; this.mealType = meal; this.amount = amount; }
    public void delete() { deleted = true; }
    public void restore() { deleted = false; }
}
