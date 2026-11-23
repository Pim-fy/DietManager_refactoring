package com.hwanghj.dietmanager.refactoring.backend.security;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import org.junit.jupiter.api.Test;
class LoginAttemptLimiterTest {
    @Test void accountLimitIsCaseInsensitiveAndExpires() {
        Clock clock=mock(Clock.class); Instant start=Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var limiter=new LoginAttemptLimiter(clock);
        for(int i=0;i<5;i++) { limiter.check("ip","User@example.test"); limiter.failed("ip","User@example.test"); }
        assertThatThrownBy(()->limiter.check("different-ip","user@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
        when(clock.instant()).thenReturn(start.plusSeconds(600));
        assertThatCode(()->limiter.check("ip","User@example.test")).doesNotThrowAnyException();
    }
    @Test void ipLimitCoversDifferentAccountsAndSuccessfulLoginResetsAccount() {
        var limiter=new LoginAttemptLimiter();
        for(int i=0;i<30;i++) limiter.failed("ip",i+"@example.test");
        assertThatThrownBy(()->limiter.check("ip","new@example.test")).isInstanceOf(LoginAttemptLimiter.Limited.class);
        for(int i=0;i<5;i++) limiter.failed("other-ip","good@example.test");
        limiter.succeeded("good@example.test");
        assertThatCode(()->limiter.check("other-ip","good@example.test")).doesNotThrowAnyException();
    }
}
