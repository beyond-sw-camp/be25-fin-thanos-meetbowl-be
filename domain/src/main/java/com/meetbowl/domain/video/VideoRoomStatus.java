package com.meetbowl.domain.video;

/** 화상회의방의 생명주기 상태다. */
public enum VideoRoomStatus {
    READY,
    ACTIVE,
    ENDING,
    ENDED,
    FAILED
}
