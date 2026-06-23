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

class GetMinutesGenerationContextUseCaseTest {

    @Test
    void returnsFinalTranscriptContext() {
        UUID meetingId = UUID.randomUUID();
        MinutesGenerationContextResult expected = context(meetingId, "첫 문장\n둘째 문장");
        GetMinutesGenerationContextUseCase useCase =
                new GetMinutesGenerationContextUseCase(id -> Optional.of(expected));

        MinutesGenerationContextResult result = useCase.execute(meetingId);

        assertEquals(expected, result);
    }

    @Test
    void rejectsContextWithoutFinalTranscript() {
        UUID meetingId = UUID.randomUUID();
        GetMinutesGenerationContextUseCase useCase =
                new GetMinutesGenerationContextUseCase(
                        id -> Optional.of(context(meetingId, " ")));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.execute(meetingId));

        assertEquals(ErrorCode.MINUTES_TRANSCRIPT_REQUIRED, exception.errorCode());
    }

    private MinutesGenerationContextResult context(UUID meetingId, String transcript) {
        UUID hostId = UUID.randomUUID();
        return new MinutesGenerationContextResult(
                meetingId,
                UUID.randomUUID(),
                hostId,
                UUID.randomUUID(),
                "주간 회의",
                Instant.parse("2026-06-23T01:00:00Z"),
                Instant.parse("2026-06-23T02:00:00Z"),
                List.of(new MinutesGenerationContextResult.Participant(hostId, "홍길동", null)),
                transcript);
    }
}
