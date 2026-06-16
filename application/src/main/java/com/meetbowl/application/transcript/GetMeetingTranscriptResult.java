package com.meetbowl.application.transcript;

import java.util.List;
import java.util.UUID;

public record GetMeetingTranscriptResult(
        UUID meetingId, String fullText, List<TranscriptSegmentResult> segments) {}
