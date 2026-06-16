package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

public record EndMeetingResult(
        UUID meetingId,
        String status,
        Instant startedAt,
        Instant endedAt,
        boolean meetingEndedEventPublished) {}
