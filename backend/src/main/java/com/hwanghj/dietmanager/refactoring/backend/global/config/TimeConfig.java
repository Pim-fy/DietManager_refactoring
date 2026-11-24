package com.hwanghj.dietmanager.refactoring.backend.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class TimeConfig {
    @Bean public Clock applicationClock() { return Clock.systemDefaultZone(); }
    @Bean
    public DateTimeProvider auditingTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
    @Bean
    public ValidationConfigurationCustomizer validationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
