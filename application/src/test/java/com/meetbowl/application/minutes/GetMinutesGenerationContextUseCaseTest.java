package com.meetbowl.application.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.MinutesGenerationContext;

class GetMinutesGenerationContextUseCaseTest {

    @Test
    void returnsFinalTranscriptContext() {
        UUID meetingId = UUID.randomUUID();
        MinutesGenerationContext expected = context(meetingId, "첫 문장\n둘째 문장");
        GetMinutesGenerationContextUseCase useCase =
                new GetMinutesGenerationContextUseCase(id -> Optional.of(expected));

        MinutesGenerationContextResult result = useCase.execute(meetingId);

        assertEquals(expected.meetingId(), result.meetingId());
        assertEquals(expected.organizationId(), result.organizationId());
        assertEquals(expected.hostUserId(), result.hostUserId());
        assertEquals(expected.reviewerUserId(), result.reviewerUserId());
        assertEquals(expected.title(), result.title());
        assertEquals(expected.startedAt(), result.startedAt());
        assertEquals(expected.endedAt(), result.endedAt());
        assertEquals(expected.segments().size(), result.segments().size());
        assertEquals(expected.segments().getFirst().sourceText(), result.segments().getFirst().sourceText());
        assertEquals(expected.rawTranscript(), result.rawTranscript());
        assertEquals(expected.participants().size(), result.participants().size());
    }

    @Test
    void rejectsContextWithoutFinalTranscript() {
        UUID meetingId = UUID.randomUUID();
        GetMinutesGenerationContextUseCase useCase =
                new GetMinutesGenerationContextUseCase(id -> Optional.of(context(meetingId, " ")));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.execute(meetingId));

        assertEquals(ErrorCode.MINUTES_TRANSCRIPT_REQUIRED, exception.errorCode());
    }

    private MinutesGenerationContext context(UUID meetingId, String transcript) {
        UUID hostId = UUID.randomUUID();
        return new MinutesGenerationContext(
                meetingId,
                UUID.randomUUID(),
                hostId,
                UUID.randomUUID(),
                "주간 회의",
                Instant.parse("2026-06-23T01:00:00Z"),
                Instant.parse("2026-06-23T02:00:00Z"),
                List.of(new MinutesGenerationContext.Participant(hostId, "홍길동", null)),
                List.of(
                        new MinutesGenerationContext.TranscriptSegment(
                                "segment-1", 1L, "KO", "첫 문장", 0L, 500L),
                        new MinutesGenerationContext.TranscriptSegment(
                                "segment-2", 2L, "KO", "둘째 문장", 600L, 1_000L)),
                transcript);
    }
}
