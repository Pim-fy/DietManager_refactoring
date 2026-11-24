package com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계정 또는 접속 주소별 로그인 시도 횟수.
 * startedAt(UTC)부터 제한 시간 동안 센다. 제한 시간이 꽉 차면(startedAt + 10분 <= 지금) 만료되어 새 기간으로 다시 센다.
 * 로그인 요청은 행 잠금으로, 정리 작업은 version(낙관적 잠금)으로 동시 수정을 막는다.
 */
@Entity
@Table(name = "login_failures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginFailure {

    @Id
    @Column(name = "failure_key", length = 80)
    private String key;

    @Column(name = "failure_count", nullable = false)
    private int count;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    // 새 행은 null이라 저장 시 INSERT(persist)가 된다. 동시에 같은 키를 만들면 중복키 오류로 드러난다.
    @Version
    @Column(nullable = false)
    private Long version;

    public LoginFailure(String key, LocalDateTime startedAt) {
        this.key = key;
        this.count = 0;
        this.startedAt = startedAt;
    }

    /** 제한 시간 안의 기록인지. */
    public boolean isActive(LocalDateTime windowStart) {
        return startedAt.isAfter(windowStart);
    }

    /** 제한 시간 안에서 센 횟수. 만료됐으면 0. */
    public int activeCount(LocalDateTime windowStart) {
        return isActive(windowStart) ? count : 0;
    }

    /** 시도를 한 번 센다. 만료된 기록이면 새 기간으로 시작한다. */
    public void increment(LocalDateTime now, LocalDateTime windowStart) {
        if (!isActive(windowStart)) {
            count = 0;
            startedAt = now;
        }
        count++;
    }

    /** 성공한 시도를 되돌린다. */
    public void decrement() {
        if (count > 0)
            count--;
    }

    /** 제한이 풀리기까지 남은 초(올림, 최소 1초). */
    public long secondsUntilReset(LocalDateTime now, long windowSeconds) {
        long millis = Duration.between(now, startedAt.plusSeconds(windowSeconds)).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }
}
