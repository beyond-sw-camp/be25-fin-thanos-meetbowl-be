package com.meetbowl.domain.video;

/** 게스트 회의 접근 세션의 상태다. */
public enum GuestSessionStatus {
    ISSUED,
    JOINED,
    LEFT,
    EXPIRED,
    REVOKED
}
