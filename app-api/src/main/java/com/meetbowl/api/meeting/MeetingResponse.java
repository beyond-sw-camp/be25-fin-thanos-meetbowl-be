package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.meeting.MeetingResult;

/** 회의 응답 DTO다. Application의 MeetingResult를 API 표현으로 변환한다. */
public record MeetingResponse(
        UUID meetingId,
        String title,
        Instant scheduledAt,
        Instant scheduledEndAt,
        UUID hostUserId,
        UUID meetingRoomId,
        String provider,
        String providerRoomId,
        String status,
        Instant startedAt,
        Instant endedAt,
        List<AttendeeResponse> attendees) {

    public static MeetingResponse from(MeetingResult result) {
        return new MeetingResponse(
                result.meetingId(),
                result.title(),
                result.scheduledAt(),
                result.scheduledEndAt(),
                result.hostUserId(),
                result.meetingRoomId(),
                result.provider(),
                result.providerRoomId(),
                result.status(),
                result.startedAt(),
                result.endedAt(),
                result.attendees().stream().map(AttendeeResponse::from).toList());
    }
}
