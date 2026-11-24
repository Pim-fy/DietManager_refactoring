package com.hwanghj.dietmanager.refactoring.backend.domain.food.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodApiUsageDto;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.entity.FoodApiUsage;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiLimitException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.repository.FoodApiUsageRepository;

@Service
public class FoodApiQuota {
    private final Clock clock;
    private final FoodApiUsageRepository repository;
    private final int minuteLimit, dayLimit;
    private final TransactionTemplate transaction;
    private final ArrayDeque<Instant> calls = new ArrayDeque<>();

    @Autowired
    public FoodApiQuota(FoodApiUsageRepository repository,
            @Value("${food-nutrition.minute-limit:60}") int minuteLimit,
            @Value("${food-nutrition.day-limit:9000}") int dayLimit,
            PlatformTransactionManager manager, Clock clock) {
        this.clock = clock;
        if (minuteLimit < 1 || dayLimit < 1)
            throw new IllegalArgumentException("식품 API 한도는 양수여야 합니다.");
        this.repository = repository;
        this.minuteLimit = minuteLimit;
        this.dayLimit = dayLimit;
        this.transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public FoodApiQuota(FoodApiUsageRepository repository, int minuteLimit, int dayLimit, PlatformTransactionManager manager) {
        this(repository, minuteLimit, dayLimit, manager, Clock.systemDefaultZone());
    }

    public synchronized void reserve() {
        prune();
        LocalDate day = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        transaction.executeWithoutResult(status -> {
            var usage = repository.findById(day).orElseGet(() -> new FoodApiUsage(day));
            if (calls.size() >= minuteLimit || usage.getCalls() >= dayLimit)
                throw new FoodApiLimitException();
            usage.increment();
            repository.saveAndFlush(usage);
        });
        calls.addLast(Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public synchronized FoodApiUsageDto.Status status() {
        prune();
        LocalDate day = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        return FoodApiUsageDto.Status.builder()
                .date(day)
                .calls(repository.findById(day).map(FoodApiUsage::getCalls).orElse(0))
                .dayLimit(dayLimit)
                .recentMinuteCalls(calls.size())
                .minuteLimit(minuteLimit)
                .build();
    }

    private void prune() {
        Instant cutoff = Instant.now(clock).minusSeconds(60);
        while (!calls.isEmpty() && !calls.peekFirst().isAfter(cutoff))
            calls.removeFirst();
    }
}
