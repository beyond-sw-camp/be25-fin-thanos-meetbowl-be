package com.meetbowl.domain.video;

/** 화상회의방이 종료된 이유다. */
public enum VideoRoomEndReason {
    HOST_ENDED,
    ALL_LEFT,
    SCHEDULE_ENDED,
    SYSTEM_FAILED
}
