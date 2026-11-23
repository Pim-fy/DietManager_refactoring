package com.hwanghj.dietmanager.refactoring.backend.security;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import org.springframework.stereotype.Component;
@Component
public class LoginAttemptLimiter {
    public static class Limited extends RuntimeException {
        public Limited() { super("로그인 실패가 반복되어 잠시 제한되었습니다. 10분 후 다시 시도해주세요."); }
    }
    private record Window(int count,Instant started) {}
    private final Map<String,Window> failures=new HashMap<>();
    private final Clock clock;
    public LoginAttemptLimiter() { this(Clock.systemUTC()); }
    @org.springframework.beans.factory.annotation.Autowired
    public LoginAttemptLimiter(Clock clock) { this.clock=clock; }
    public synchronized void check(String ip,String email) {
        failures.entrySet().removeIf(entry->!entry.getValue().started().plusSeconds(600).isAfter(clock.instant()));
        if(failures.size()>=10000 || count("ip:"+ip)>=30 || count(key(email))>=5) throw new Limited();
    }
    public synchronized void failed(String ip,String email) { increment("ip:"+ip); increment(key(email)); }
    public synchronized void succeeded(String email) { failures.remove(key(email)); }
    private int count(String key) { return failures.getOrDefault(key,new Window(0,clock.instant())).count(); }
    private void increment(String key) {
        var previous=failures.getOrDefault(key,new Window(0,clock.instant()));
        if(failures.containsKey(key) || failures.size()<10000) failures.put(key,new Window(previous.count()+1,previous.started()));
    }
    private String key(String email) {
        try { return "account:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 없음"); }
    }
}
