package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hwanghj.dietmanager.refactoring.backend.global.error.BusinessException;
import com.hwanghj.dietmanager.refactoring.backend.global.error.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Component
public class LoginAttemptLimiter {
    public static class Limited extends BusinessException {
        public Limited() { super(ErrorCode.LOGIN_LIMIT); }
    }
    @Getter
    @AllArgsConstructor
    private static class Window {
        private final int count;
        private final Instant started;
    }
    static final int MAX_ENTRIES=10000;
    // 삽입 순서 = 기간 시작 순서. 가득 차면 가장 오래된 항목부터 지운다.
    private final Map<String,Window> failures=new LinkedHashMap<>();
    private final Clock clock;
    public LoginAttemptLimiter() { this(Clock.systemUTC()); }
    @Autowired
    public LoginAttemptLimiter(Clock clock) { this.clock=clock; }
    public synchronized void check(String ip,String email) {
        failures.entrySet().removeIf(entry->!entry.getValue().getStarted().plusSeconds(600).isAfter(clock.instant()));
        if(count("ip:"+ip)>=30 || count(key(email))>=5) throw new Limited();
    }
    public synchronized void failed(String ip,String email) { increment("ip:"+ip); increment(key(email)); }
    public synchronized void succeeded(String email) { failures.remove(key(email)); }
    private int count(String key) { return failures.getOrDefault(key,new Window(0,clock.instant())).getCount(); }
    private void increment(String key) {
        var previous=failures.getOrDefault(key,new Window(0,clock.instant()));
        if(!failures.containsKey(key) && failures.size()>=MAX_ENTRIES) failures.remove(failures.keySet().iterator().next());
        failures.put(key,new Window(previous.getCount()+1,previous.getStarted()));
    }
    private String key(String email) {
        try { return "account:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 없음"); }
    }
}
