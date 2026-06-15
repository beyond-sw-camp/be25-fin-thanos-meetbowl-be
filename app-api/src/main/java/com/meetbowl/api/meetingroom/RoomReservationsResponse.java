package com.meetbowl.api.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.meetingroom.RoomReservationsResult;

/** 회의실 예약 타임라인 응답 DTO다(F4 전체 현황). 회의실 한 행 + 그 회의실의 예약 블록들. */
public record RoomReservationsResponse(
        UUID roomId,
        String name,
        int capacity,
        boolean available,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        List<ReservationBlock> reservations) {

    /** 예약 블록 한 건. {@code hostUserId}로 FE가 "내 예약/예약됨"을 구분한다. */
    public record ReservationBlock(
            UUID meetingId,
            String title,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID hostUserId) {}

    public static RoomReservationsResponse from(RoomReservationsResult result) {
        List<ReservationBlock> blocks =
                result.reservations().stream()
                        .map(
                                reservation ->
                                        new ReservationBlock(
                                                reservation.meetingId(),
                                                reservation.title(),
                                                reservation.scheduledAt(),
                                                reservation.scheduledEndAt(),
                                                reservation.hostUserId()))
                        .toList();
        return new RoomReservationsResponse(
                result.roomId(),
                result.name(),
                result.capacity(),
                result.available(),
                result.siteId(),
                result.siteName(),
                result.buildingId(),
                result.buildingName(),
                blocks);
    }
}
