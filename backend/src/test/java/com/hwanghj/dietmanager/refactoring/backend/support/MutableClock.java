package com.hwanghj.dietmanager.refactoring.backend.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** 테스트에서 현재 시각을 바꿀 수 있는 시계. 가입일·토큰·업무 날짜가 모두 이 시계를 따른다. */
public class MutableClock extends Clock {
    private volatile Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void set(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new ZonedView(this, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /** 같은 시각을 다른 시간대로 보여준다(MutableClock 변경을 그대로 따른다). */
    private static class ZonedView extends Clock {
        private final MutableClock source;
        private final ZoneId zone;

        ZonedView(MutableClock source, ZoneId zone) {
            this.source = source;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new ZonedView(source, zone);
        }

        @Override
        public Instant instant() {
            return source.instant();
        }
    }
}
