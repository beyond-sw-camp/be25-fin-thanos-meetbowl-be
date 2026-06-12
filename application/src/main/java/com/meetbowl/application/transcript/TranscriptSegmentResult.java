package com.meetbowl.application.transcript;

public record TranscriptSegmentResult(
        String segmentId,
        long sequence,
        String language,
        String sourceText,
        Long startedAtMs,
        Long endedAtMs) {}
