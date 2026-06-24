package com.meetbowl.api.minutes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.minutes.MinutesGenerationContextResult;

/** AI 서버 전용 회의록 생성 Context 응답이다. */
public record MinutesGenerationContextResponse(
        UUID meetingId,
        UUID organizationId,
        UUID hostUserId,
        UUID reviewerUserId,
        String title,
        Instant startedAt,
        Instant endedAt,
        List<ParticipantResponse> participants,
        List<TranscriptSegmentResponse> segments,
        String rawTranscript) {

    public static MinutesGenerationContextResponse from(MinutesGenerationContextResult result) {
        return new MinutesGenerationContextResponse(
                result.meetingId(),
                result.organizationId(),
                result.hostUserId(),
                result.reviewerUserId(),
                result.title(),
                result.startedAt(),
                result.endedAt(),
                result.participants().stream().map(ParticipantResponse::from).toList(),
                result.segments().stream().map(TranscriptSegmentResponse::from).toList(),
                result.rawTranscript());
    }

    public record ParticipantResponse(UUID userId, String name, String department) {
        static ParticipantResponse from(MinutesGenerationContextResult.Participant participant) {
            return new ParticipantResponse(
                    participant.userId(), participant.name(), participant.department());
        }
    }

    public record TranscriptSegmentResponse(
            String segmentId,
            long sequence,
            String language,
            String sourceText,
            Long startedAtMs,
            Long endedAtMs) {
        static TranscriptSegmentResponse from(MinutesGenerationContextResult.TranscriptSegment segment) {
            return new TranscriptSegmentResponse(
                    segment.segmentId(),
                    segment.sequence(),
                    segment.language(),
                    segment.sourceText(),
                    segment.startedAtMs(),
                    segment.endedAtMs());
        }
    }
}
