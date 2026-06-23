package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.UUID;

/** 내 회의실 예약 항목 출력 모델이다(F4 /me). "내가 예약한 회의실 / 내가 참석할 회의" 목록의 한 줄에 대응하며, 회의실명을 함께 담는다. */
public record ReservationItemResult(
        UUID meetingId,
        UUID roomId,
        String roomName,
        String title,
        Instant scheduledAt,
        Instant scheduledEndAt,
        UUID hostUserId) {}
