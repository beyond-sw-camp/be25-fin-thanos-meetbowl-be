package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 회의실 예약 타임라인 항목 출력 모델이다(F4 전체 현황). 회의실 한 개와, 조회 시간대에 그 회의실에 잡힌 예약 블록 및 관리자 차단 구간을 담는다. 타임라인 그리드의 한
 * 행에 대응한다.
 */
public record RoomReservationsResult(
        UUID roomId,
        String name,
        int capacity,
        boolean available,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        List<Reservation> reservations,
        List<Block> blocks) {

    /** 예약 블록 한 건. {@code hostUserId}로 FE가 "내 예약/예약됨"을 구분한다. */
    public record Reservation(
            UUID meetingId,
            String title,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID hostUserId) {}

    /** 관리자가 막아둔 시간대 차단 한 건. FE는 타임라인에 "사용 불가(회색)"로 그린다. */
    public record Block(UUID blockId, Instant startAt, Instant endAt, String reason) {}
}
