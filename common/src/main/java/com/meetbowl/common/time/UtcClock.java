package com.meetbowl.common.time;

import java.time.Clock;

public final class UtcClock {

    private UtcClock() {
    }

    public static Clock system() {
        return Clock.systemUTC();
    }
}
