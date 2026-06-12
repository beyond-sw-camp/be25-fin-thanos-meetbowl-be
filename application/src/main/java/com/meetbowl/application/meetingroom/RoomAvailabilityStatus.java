package com.meetbowl.application.meetingroom;

/**
 * 회의실 현황 상태다(F3).
 *
 * <ul>
 *   <li>{@code AVAILABLE} — 조회 시간대에 점유 회의가 없어 예약 가능
 *   <li>{@code RESERVED} — 조회 시간대에 예정(SCHEDULED) 회의가 있어 예약됨(시작 전)
 *   <li>{@code IN_USE} — 조회 시간대에 진행 중(IN_PROGRESS) 회의가 있어 사용 중
 *   <li>{@code UNAVAILABLE} — 관리자가 사용 불가로 설정한 회의실
 * </ul>
 */
public enum RoomAvailabilityStatus {
    AVAILABLE,
    RESERVED,
    IN_USE,
    UNAVAILABLE
}