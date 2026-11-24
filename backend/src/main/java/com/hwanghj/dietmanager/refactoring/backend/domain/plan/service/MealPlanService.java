package com.hwanghj.dietmanager.refactoring.backend.domain.plan.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodLibraryService;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.IntakeRecord;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.repository.IntakeRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.service.IntakeMapper;
import com.hwanghj.dietmanager.refactoring.backend.domain.plan.dto.MealPlanDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.plan.entity.MealPlan;
import com.hwanghj.dietmanager.refactoring.backend.domain.plan.exception.MealPlanConflictException;
import com.hwanghj.dietmanager.refactoring.backend.domain.plan.exception.MealPlanNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.plan.repository.MealPlanRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

@Service
public class MealPlanService {
    private final Clock clock;
    private final MealPlanRepository plans;
    private final FoodLibraryService foods;
    private final UserRepository users;
    private final IntakeRepository intakes;

    @Autowired
    public MealPlanService(MealPlanRepository plans, FoodLibraryService foods,
            UserRepository users, IntakeRepository intakes, Clock clock) {
        this.plans = plans;
        this.foods = foods;
        this.users = users;
        this.intakes = intakes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MealPlanDto.Response> daily(Long userId, LocalDate date) {
        return plans.findByUserIdAndPlannedDateOrderByIdAsc(userId, date).stream()
                .map(this::response).toList();
    }

    @Transactional
    public MealPlanDto.Response create(Long userId, MealPlanDto.Request request) {
        var account = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var food = foods.resolve(userId, request.getFoodCode());
        return response(plans.save(new MealPlan(account, food, request.getPlannedDate(),
                request.getMealType(), request.getAmount())));
    }

    @Transactional
    public MealPlanDto.Response update(Long userId, Long id, MealPlanDto.Request request) {
        var plan = owned(userId, id);
        editable(plan);
        if (!plan.getSnapshot().food().getFoodCode().equals(request.getFoodCode())) {
            throw new MealPlanConflictException("계획의 식품 변경은 새 계획으로 추가해주세요.");
        }
        plan.update(request.getPlannedDate(), request.getMealType(), request.getAmount());
        return response(plan);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        owned(userId, id).delete();
    }

    @Transactional
    public MealPlanDto.Response restore(Long userId, Long id) {
        var plan = owned(userId, id);
        plan.restore();
        return response(plan);
    }

    @Transactional
    public MealPlanDto.Response consume(Long userId, Long id) {
        // 소유권 조회의 비관적 잠금을 섭취 저장·계획 전환까지 유지한다.
        var plan = owned(userId, id);
        editable(plan);
        if (plan.getPlannedDate().isAfter(LocalDate.now(clock))) {
            throw new MealPlanConflictException("미래 날짜의 계획은 아직 섭취로 기록할 수 없습니다.");
        }
        var food = plan.getSnapshot().food();
        var intake = intakes.save(new IntakeRecord(plan.getUser(), food, plan.getPlannedDate(),
                plan.getMealType(), plan.getAmount()));
        foods.used(userId, food);
        plan.consumed(intake.getId());
        return response(plan);
    }

    private MealPlan owned(Long userId, Long id) {
        return plans.findByIdAndUserId(id, userId).orElseThrow(MealPlanNotFoundException::new);
    }

    private void editable(MealPlan plan) {
        if (plan.isDeleted() || plan.getIntakeId() != null) {
            throw new MealPlanConflictException("삭제되었거나 이미 섭취로 기록한 계획은 변경할 수 없습니다.");
        }
    }

    private MealPlanDto.Response response(MealPlan plan) {
        var food = plan.getSnapshot().food();
        return MealPlanDto.Response.builder()
                .id(plan.getId())
                .plannedDate(plan.getPlannedDate())
                .mealType(plan.getMealType())
                .amount(plan.getAmount())
                .food(food)
                .nutrition(IntakeMapper.preview(food, plan.getPlannedDate(), plan.getMealType(), plan.getAmount()))
                .deleted(plan.isDeleted())
                .intakeId(plan.getIntakeId())
                .build();
    }
}
