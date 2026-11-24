package com.hwanghj.dietmanager.refactoring.backend.domain.food;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiLimitException;
class FoodApiQuotaTest {
    final FoodApiUsageRepository repository=mock(FoodApiUsageRepository.class);
    final PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    final Map<LocalDate,FoodApiUsage> saved=new HashMap<>();
    FoodApiQuota quota(int minute,int day) {
        when(manager.getTransaction(any())).thenAnswer(invocation->new SimpleTransactionStatus());
        when(repository.findById(any())).thenAnswer(invocation->Optional.ofNullable(saved.get(invocation.getArgument(0))));
        when(repository.saveAndFlush(any())).thenAnswer(invocation->{ FoodApiUsage usage=invocation.getArgument(0); saved.put(usage.getDate(),usage); return usage; });
        return new FoodApiQuota(repository,minute,day,manager);
    }
    @Test void minuteLimitRejectsBeforeIncrementingDailyCounter() {
        var quota=quota(2,10); quota.reserve(); quota.reserve();
        assertThatThrownBy(quota::reserve).isInstanceOf(FoodApiLimitException.class);
        assertThat(quota.status().calls()).isEqualTo(2);
        verify(repository,times(2)).saveAndFlush(any());
    }
    @Test void dailyLimitSurvivesNewQuotaInstance() {
        var quota=quota(10,2); quota.reserve(); quota.reserve();
        var restarted=new FoodApiQuota(repository,10,2,manager);
        assertThatThrownBy(restarted::reserve).isInstanceOf(FoodApiLimitException.class);
        assertThat(restarted.status().calls()).isEqualTo(2);
        assertThat(restarted.status().recentMinuteCalls()).isZero();
    }
}
