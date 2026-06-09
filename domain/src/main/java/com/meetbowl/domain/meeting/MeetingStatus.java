package com.meetbowl.domain.meeting;

/** 회의 상태다. 화상회의 진행 흐름(예정 → 진행 → 종료)과 취소를 표현한다. */
public enum MeetingStatus {
    SCHEDULED,
    IN_PROGRESS,
    ENDED,
    CANCELLED
}
