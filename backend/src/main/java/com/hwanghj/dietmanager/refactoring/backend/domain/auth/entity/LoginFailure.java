package com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계정 또는 접속 주소별 로그인 실패 횟수.
 * startedAt(UTC)부터 제한 시간 동안 실패를 센다. 제한 시간이 꽉 차면(startedAt + 10분 <= 지금) 만료되어 새 기간으로 다시 센다.
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

    public LoginFailure(String key, LocalDateTime startedAt) {
        this.key = key;
        this.count = 0;
        this.startedAt = startedAt;
    }

    /** 제한 시간이 지난 기록이면 새 기간으로 시작하고, 실패를 한 번 더한다. */
    public void increment(LocalDateTime now, LocalDateTime windowStart) {
        if (!isActive(windowStart)) {
            count = 0;
            startedAt = now;
        }
        count++;
    }

    public boolean isActive(LocalDateTime windowStart) {
        return startedAt.isAfter(windowStart);
    }
}
