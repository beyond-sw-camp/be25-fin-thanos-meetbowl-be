package com.meetbowl.domain.video;

/** 참가자 접속 세션의 상태다. */
public enum ParticipantSessionStatus {
    JOIN_REQUESTED,
    JOINED,
    LEFT,
    DISCONNECTED,
    KICKED
}
