package com.meetbowl.application.transcript;

import java.util.UUID;

public record SaveFinalTranscriptCommand(
        UUID sourceEventId,
        UUID correlationId,
        UUID meetingId,
        UUID sessionId,
        String segmentId,
        long sequence,
        String language,
        String text,
        Long startedAtMs,
        Long endedAtMs,
        String provider,
        String idempotencyKey) {}
