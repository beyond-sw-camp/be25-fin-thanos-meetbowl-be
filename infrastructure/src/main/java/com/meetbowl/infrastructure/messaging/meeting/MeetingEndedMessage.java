package com.meetbowl.infrastructure.messaging.meeting;

import java.time.Instant;
import java.util.UUID;

/** 루트 event-contract의 meeting.ended payload를 표현하는 RabbitMQ Message DTO다. */
public record MeetingEndedMessage(
        UUID meetingId,
        UUID organizationId,
        UUID hostUserId,
        UUID reviewerUserId,
        String title,
        Instant startedAt,
        Instant endedAt) {}
