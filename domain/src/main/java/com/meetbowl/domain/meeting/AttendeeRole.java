package com.meetbowl.domain.meeting;

/**
 * 회의-사용자 연결의 구조적 신분이다. 주최자/일반 참석자를 구분한다.
 *
 * <p>"회의록 검토자"는 신분이 아니라 담당이므로 별도 속성({@link MeetingAttendee#reviewer()})으로 분리한다. 그래야 주최자(HOST)가
 * 검토자를 겸할 수 있다 — 신분(role)과 검토 담당(reviewer)은 서로 독립적인 축이다.
 */
public enum AttendeeRole {
    HOST,
    PARTICIPANT
}
