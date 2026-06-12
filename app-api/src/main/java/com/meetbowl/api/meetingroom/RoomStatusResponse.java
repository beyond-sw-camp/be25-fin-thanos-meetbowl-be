package com.meetbowl.api.meetingroom;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.meetingroom.RoomStatusResult;

/** 회의실 현황 응답 DTO다(F3). {@code status}는 AVAILABLE/RESERVED/IN_USE/UNAVAILABLE. */
public record RoomStatusResponse(
        UUID roomId,
        String name,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        int capacity,
        String status,
        CurrentMeeting currentMeeting) {

    /** 회의실을 점유한 회의 요약. 점유 회의가 없으면 null. */
    public record CurrentMeeting(UUID meetingId, Instant scheduledAt, Instant scheduledEndAt) {}

    public static RoomStatusResponse from(RoomStatusResult result) {
        CurrentMeeting currentMeeting =
                result.currentMeeting() == null
                        ? null
                        : new CurrentMeeting(
                                result.currentMeeting().meetingId(),
                                result.currentMeeting().scheduledAt(),
                                result.currentMeeting().scheduledEndAt());
        return new RoomStatusResponse(
                result.roomId(),
                result.name(),
                result.siteId(),
                result.siteName(),
                result.buildingId(),
                result.buildingName(),
                result.capacity(),
                result.status().name(),
                currentMeeting);
    }
}