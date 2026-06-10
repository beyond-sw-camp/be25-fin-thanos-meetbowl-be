package com.meetbowl.domain.meeting;

/** 회의-사용자 연결의 역할이다. 주최자/일반 참석자/회의록 검토자를 구분한다. */
public enum AttendeeRole {
    HOST,
    PARTICIPANT,
    REVIEWER
}
