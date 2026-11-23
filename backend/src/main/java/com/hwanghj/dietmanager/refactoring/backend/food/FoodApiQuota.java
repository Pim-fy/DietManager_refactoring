package com.hwanghj.dietmanager.refactoring.backend.food;
import java.time.*;
import java.util.ArrayDeque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FoodApiQuota {
    private final java.time.Clock clock;
    private final FoodApiUsageRepository repository;
    private final int minuteLimit,dayLimit;
    private final org.springframework.transaction.support.TransactionTemplate transaction;
    private final ArrayDeque<Instant> calls=new ArrayDeque<>();
    @org.springframework.beans.factory.annotation.Autowired
    public FoodApiQuota(FoodApiUsageRepository repository,
            @Value("${food-nutrition.minute-limit:60}") int minuteLimit,
            @Value("${food-nutrition.day-limit:9000}") int dayLimit,
            org.springframework.transaction.PlatformTransactionManager manager, java.time.Clock clock) {
        this.clock = clock;
        if(minuteLimit<1 || dayLimit<1) throw new IllegalArgumentException("식품 API 한도는 양수여야 합니다.");
        this.repository=repository; this.minuteLimit=minuteLimit; this.dayLimit=dayLimit;
        this.transaction=new org.springframework.transaction.support.TransactionTemplate(manager);
        transaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public FoodApiQuota(FoodApiUsageRepository repository, int minuteLimit, int dayLimit, org.springframework.transaction.PlatformTransactionManager manager) {
        this(repository, minuteLimit, dayLimit, manager, Clock.systemDefaultZone());
    }
    public synchronized void reserve() {
        prune(); LocalDate day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        transaction.executeWithoutResult(status -> {
        var usage=repository.findById(day).orElseGet(()->new FoodApiUsage(day));
        if(calls.size()>=minuteLimit || usage.getCalls()>=dayLimit) throw new FoodApiLimitException();
        usage.increment(); repository.saveAndFlush(usage);
        });
        calls.addLast(Instant.now(clock));
    }
    public record Status(LocalDate date,int calls,int dayLimit,int recentMinuteCalls,int minuteLimit) {}
    @Transactional(readOnly=true) public synchronized Status status() {
        prune(); LocalDate day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        return new Status(day,repository.findById(day).map(FoodApiUsage::getCalls).orElse(0),dayLimit,calls.size(),minuteLimit);
    }
    private void prune() { Instant cutoff=Instant.now(clock).minusSeconds(60); while(!calls.isEmpty() && !calls.peekFirst().isAfter(cutoff)) calls.removeFirst(); }
}
