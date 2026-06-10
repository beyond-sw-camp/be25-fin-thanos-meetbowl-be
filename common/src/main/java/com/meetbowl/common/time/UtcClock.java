package com.meetbowl.common.time;

import java.time.Clock;

/** 시스템 기본 시간대에 따른 결과 차이를 없애고, 시간 의존 로직이 테스트 가능한 Clock 경계를 사용하도록 둔다. */
public final class UtcClock {

    private UtcClock() {}

    public static Clock system() {
        return Clock.systemUTC();
    }
}
