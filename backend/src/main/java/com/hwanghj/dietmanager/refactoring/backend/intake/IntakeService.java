package com.hwanghj.dietmanager.refactoring.backend.intake;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.user.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.exception.UserNotFoundException;

@Service
public class IntakeService {
    private final IntakeRepository repository;
    private final UserRepository users;
    private final com.hwanghj.dietmanager.refactoring.backend.food.FoodLibraryService foods;
    public IntakeService(IntakeRepository repository, UserRepository users, com.hwanghj.dietmanager.refactoring.backend.food.FoodLibraryService foods) {
        this.repository = repository; this.users = users; this.foods = foods;
    }
    @Transactional
    public IntakeDto.Response create(Long userId, IntakeDto.CreateRequest request) {
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var food = foods.resolve(userId, request.foodCode());
        foods.used(userId, food);
        return IntakeMapper.response(repository.save(new IntakeRecord(user, food, request.eatenDate(), request.mealType(), request.amount())));
    }
    @Transactional(readOnly = true)
    public IntakeDto.DailyResponse daily(Long userId, LocalDate date) {
        var items = repository.findByUserIdAndEatenDateAndDeletedFalseOrderByIdAsc(userId, date).stream().map(IntakeMapper::response).toList();
        var deletedItems = repository.findByUserIdAndEatenDateAndDeletedTrueOrderByIdAsc(userId, date).stream().map(IntakeMapper::response).toList();
        return new IntakeDto.DailyResponse(date, items, deletedItems, IntakeMapper.total(items, IntakeDto.Response::calories), IntakeMapper.total(items, IntakeDto.Response::carbohydrate),
            IntakeMapper.total(items, IntakeDto.Response::protein), IntakeMapper.total(items, IntakeDto.Response::fat));
    }
    @Transactional
    public IntakeDto.Response createManual(Long userId, IntakeDto.ManualRequest request) {
        var user=users.findById(userId).orElseThrow(UserNotFoundException::new);
        var input=request.nutrition();
        var food=new com.hwanghj.dietmanager.refactoring.backend.food.FoodNutrition("manual:"+java.util.UUID.randomUUID(),
            input.foodName().trim(),"",input.baseAmount()+request.unit(),input.baseAmount(),request.unit(),
            input.calories(),input.carbohydrate(),input.protein(),input.fat(),"");
        var record=new IntakeRecord(user,food,request.eatenDate(),request.mealType(),request.amount());
        record.markManual(); return IntakeMapper.response(repository.save(record));
    }
    @Transactional
    public IntakeDto.Response editNutrition(Long userId,Long id,IntakeDto.NutritionInput input) {
        var record=owned(userId,id);
        if(record.isDeleted()) throw new IntakeException("삭제된 기록은 먼저 복구해주세요.");
        record.editNutrition(input); return IntakeMapper.response(record);
    }
    @Transactional
    public IntakeDto.Response update(Long userId, Long id, IntakeDto.UpdateRequest request) {
        var record = owned(userId, id);
        if (record.isDeleted()) throw new IntakeException("삭제된 기록은 먼저 복구해주세요.");
        record.update(request.eatenDate(), request.mealType(), request.amount()); return IntakeMapper.response(record);
    }
    @Transactional
    public void delete(Long userId, Long id) { owned(userId, id).delete(); }
    @Transactional
    public IntakeDto.Response restore(Long userId, Long id) { var record = owned(userId, id); record.restore(); return IntakeMapper.response(record); }
    private IntakeRecord owned(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(IntakeNotFoundException::new);
    }
}
