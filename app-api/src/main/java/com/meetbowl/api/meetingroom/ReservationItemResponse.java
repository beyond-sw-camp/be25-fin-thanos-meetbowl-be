package com.meetbowl.api.meetingroom;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.meetingroom.ReservationItemResult;

/** 내 회의실 예약 항목 응답 DTO다(F4 /me). */
public record ReservationItemResponse(
        UUID meetingId,
        UUID roomId,
        String roomName,
        String title,
        Instant scheduledAt,
        Instant scheduledEndAt,
        UUID hostUserId) {

    public static ReservationItemResponse from(ReservationItemResult result) {
        return new ReservationItemResponse(
                result.meetingId(),
                result.roomId(),
                result.roomName(),
                result.title(),
                result.scheduledAt(),
                result.scheduledEndAt(),
                result.hostUserId());
    }
}
