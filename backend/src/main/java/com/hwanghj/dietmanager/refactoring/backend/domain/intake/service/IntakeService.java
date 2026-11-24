package com.hwanghj.dietmanager.refactoring.backend.domain.intake.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodLibraryService;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.dto.IntakeDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.entity.IntakeRecord;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.exception.IntakeException;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.exception.IntakeNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.repository.IntakeRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.exception.UserNotFoundException;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;

@Service
public class IntakeService {
    private final IntakeRepository repository;
    private final UserRepository users;
    private final FoodLibraryService foods;

    public IntakeService(IntakeRepository repository, UserRepository users, FoodLibraryService foods) {
        this.repository = repository;
        this.users = users;
        this.foods = foods;
    }

    @Transactional
    public IntakeDto.Response create(Long userId, IntakeDto.CreateRequest request) {
        var food = foods.resolve(userId, request.getFoodCode());
        // 외부 식품 조회가 끝난 뒤, 다른 저장보다 먼저 users 행을 잠근다(최근 사용 식품의 처음 저장).
        var user = users.findWithLockById(userId).orElseThrow(UserNotFoundException::new);
        foods.used(userId, food);
        return IntakeMapper.response(repository.save(new IntakeRecord(user, food, request.getEatenDate(), request.getMealType(), request.getAmount())));
    }

    @Transactional(readOnly = true)
    public IntakeDto.DailyResponse daily(Long userId, LocalDate date) {
        var items = repository.findByUserIdAndEatenDateAndDeletedFalseOrderByIdAsc(userId, date).stream().map(IntakeMapper::response).toList();
        var deletedItems = repository.findByUserIdAndEatenDateAndDeletedTrueOrderByIdAsc(userId, date).stream().map(IntakeMapper::response).toList();
        return IntakeDto.DailyResponse.builder()
                .date(date)
                .items(items)
                .deletedItems(deletedItems)
                .calories(IntakeMapper.total(items, IntakeDto.Response::getCalories))
                .carbohydrate(IntakeMapper.total(items, IntakeDto.Response::getCarbohydrate))
                .protein(IntakeMapper.total(items, IntakeDto.Response::getProtein))
                .fat(IntakeMapper.total(items, IntakeDto.Response::getFat))
                .build();
    }

    @Transactional
    public IntakeDto.Response createManual(Long userId, IntakeDto.ManualRequest request) {
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var input = request.getNutrition();
        var food = FoodNutrition.builder()
                .foodCode("manual:" + UUID.randomUUID())
                .foodName(input.getFoodName().trim())
                .makerName("")
                .servingSize(input.getBaseAmount() + request.getUnit())
                .baseAmount(input.getBaseAmount())
                .baseUnit(request.getUnit())
                .calories(input.getCalories())
                .carbohydrate(input.getCarbohydrate())
                .protein(input.getProtein())
                .fat(input.getFat())
                .sourceUpdatedDate("")
                .build();
        var record = new IntakeRecord(user, food, request.getEatenDate(), request.getMealType(), request.getAmount());
        record.markManual();
        return IntakeMapper.response(repository.save(record));
    }

    @Transactional
    public IntakeDto.Response editNutrition(Long userId, Long id, IntakeDto.NutritionInput input) {
        var record = owned(userId, id);
        if (record.isDeleted())
            throw new IntakeException("삭제된 기록은 먼저 복구해주세요.");
        record.editNutrition(input);
        return IntakeMapper.response(record);
    }

    @Transactional
    public IntakeDto.Response update(Long userId, Long id, IntakeDto.UpdateRequest request) {
        var record = owned(userId, id);
        if (record.isDeleted())
            throw new IntakeException("삭제된 기록은 먼저 복구해주세요.");
        record.update(request.getEatenDate(), request.getMealType(), request.getAmount());
        return IntakeMapper.response(record);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        owned(userId, id).delete();
    }

    @Transactional
    public IntakeDto.Response restore(Long userId, Long id) {
        var record = owned(userId, id);
        record.restore();
        return IntakeMapper.response(record);
    }

    private IntakeRecord owned(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(IntakeNotFoundException::new);
    }
}
