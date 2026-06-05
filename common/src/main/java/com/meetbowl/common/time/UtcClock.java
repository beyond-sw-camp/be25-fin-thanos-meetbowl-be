package com.meetbowl.common.time;

import java.time.Clock;

/**
 * 서버 내부 시간 기준은 UTC로 고정한다.
 * 테스트에서는 Clock을 주입받는 구조로 확장할 수 있게 Clock 타입을 반환한다.
 */
public final class UtcClock {

    private UtcClock() {
    }

    public static Clock system() {
        return Clock.systemUTC();
    }
}
