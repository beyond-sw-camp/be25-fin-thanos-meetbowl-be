package com.meetbowl.domain.meetingroom;

/** 회의실 예약 상태다. 취소는 행 삭제 대신 상태 전환(soft cancel)으로 처리한다. enum 정해진 값만 들어갈 수 있음 */
public enum ReservationStatus {
    RESERVED,
    CANCELLED
}
