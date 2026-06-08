package com.meetbowl.domain.video;

/** 참가자 세션이 종료된 이유다. */
public enum ParticipantLeaveReason {
    USER_LEFT,
    NETWORK_LOST,
    HOST_REMOVED,
    ROOM_ENDED
}
