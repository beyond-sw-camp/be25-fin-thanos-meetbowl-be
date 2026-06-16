package com.meetbowl.api.meeting.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.meeting.EndMeetingResult;

public record EndMeetingResponse(
        UUID meetingId,
        String status,
        Instant startedAt,
        Instant endedAt,
        boolean meetingEndedEventPublished) {

    public static EndMeetingResponse from(EndMeetingResult result) {
        return new EndMeetingResponse(
                result.meetingId(),
                result.status(),
                result.startedAt(),
                result.endedAt(),
                result.meetingEndedEventPublished());
    }
}
