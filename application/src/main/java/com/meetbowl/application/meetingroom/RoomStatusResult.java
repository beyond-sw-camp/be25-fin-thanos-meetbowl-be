package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.UUID;

/**
 * 회의실 현황 항목 출력 모델이다(F3). 조회 시간대 기준의 상태와, 그 상태를 만든 점유 회의({@code currentMeeting})를 담는다. 점유 회의가 없으면
 * {@code currentMeeting}은 null이다.
 */
public record RoomStatusResult(
        UUID roomId,
        String name,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        int capacity,
        RoomAvailabilityStatus status,
        OccupyingMeeting currentMeeting) {

    /** 회의실을 점유한 회의 요약이다. */
    public record OccupyingMeeting(UUID meetingId, Instant scheduledAt, Instant scheduledEndAt) {}
}