package com.meetbowl.api.meeting.dto;

import java.util.List;
import java.util.UUID;

import com.meetbowl.application.transcript.GetMeetingTranscriptResult;
import com.meetbowl.application.transcript.TranscriptSegmentResult;

public record MeetingTranscriptResponse(
        UUID meetingId, String fullText, List<MeetingTranscriptSegmentResponse> segments) {

    public static MeetingTranscriptResponse from(GetMeetingTranscriptResult result) {
        return new MeetingTranscriptResponse(
                result.meetingId(),
                result.fullText(),
                result.segments().stream()
                        .map(MeetingTranscriptSegmentResponse::from)
                        .toList());
    }

    public record MeetingTranscriptSegmentResponse(
            String segmentId,
            long sequence,
            String language,
            String sourceText,
            Long startedAtMs,
            Long endedAtMs) {

        static MeetingTranscriptSegmentResponse from(TranscriptSegmentResult result) {
            return new MeetingTranscriptSegmentResponse(
                    result.segmentId(),
                    result.sequence(),
                    result.language(),
                    result.sourceText(),
                    result.startedAtMs(),
                    result.endedAtMs());
        }
    }
}
