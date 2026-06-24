package com.meetbowl.domain.meeting;

/**
 * 회의-사용자 연결의 구조적 신분이다. 주최자/일반 참석자를 구분한다.
 *
 * <p>"회의록 검토자"는 신분이 아니라 담당이므로 별도 속성({@link MeetingAttendee#reviewer()})으로 분리한다. 그래야 주최자(HOST)가
 * 검토자를 겸할 수 있다 — 신분(role)과 검토 담당(reviewer)은 서로 독립적인 축이다.
 *
 * <p>{@code REVIEWER}는 레거시 DB 호환을 위해 유지한다. 신규 저장은 여전히 {@code reviewer} 플래그로만 처리하고, role 자체는
 * HOST/PARTICIPANT 중심으로 사용한다.
 */
public enum AttendeeRole {
    HOST,
    PARTICIPANT,
    REVIEWER
}
