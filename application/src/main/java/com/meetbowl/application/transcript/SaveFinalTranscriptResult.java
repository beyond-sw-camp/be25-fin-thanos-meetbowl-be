package com.meetbowl.application.transcript;

import java.util.UUID;

public record SaveFinalTranscriptResult(
        UUID meetingId, String segmentId, long sequence, boolean saved, UUID sourceEventId) {}
