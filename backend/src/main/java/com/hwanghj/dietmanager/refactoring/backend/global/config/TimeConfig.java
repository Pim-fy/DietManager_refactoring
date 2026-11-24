package com.hwanghj.dietmanager.refactoring.backend.global.config;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class TimeConfig {
    @Bean public Clock applicationClock() { return Clock.systemDefaultZone(); }
    @Bean
    public org.springframework.data.auditing.DateTimeProvider auditingTimeProvider(Clock clock) {
        return () -> java.util.Optional.of(java.time.LocalDateTime.now(clock));
    }
    @Bean
    public org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer validationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
