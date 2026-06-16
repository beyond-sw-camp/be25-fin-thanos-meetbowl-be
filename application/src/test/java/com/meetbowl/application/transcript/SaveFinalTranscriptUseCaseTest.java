package com.meetbowl.application.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;

class SaveFinalTranscriptUseCaseTest {

    @Test
    void 새FinalTranscript는저장한다() {
        InMemoryTranscriptRepository repository = new InMemoryTranscriptRepository();
        SaveFinalTranscriptUseCase useCase = new SaveFinalTranscriptUseCase(repository);
        UUID eventId = UUID.randomUUID();

        SaveFinalTranscriptResult result =
                useCase.execute(
                        new SaveFinalTranscriptCommand(
                                eventId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "segment-1",
                                1L,
                                "unknown",
                                "안녕하세요",
                                10L,
                                20L,
                                "openai-realtime-transcription",
                                "segment-1"));

        assertTrue(result.saved());
        assertEquals(1, repository.segments.size());
        assertEquals("안녕하세요", repository.segments.getFirst().sourceText());
        assertEquals("UNKNOWN", repository.segments.getFirst().sourceLanguage().name());
    }

    @Test
    void 같은이벤트는중복저장하지않는다() {
        UUID eventId = UUID.randomUUID();
        InMemoryTranscriptRepository repository = new InMemoryTranscriptRepository();
        repository.eventIds.add(eventId);
        SaveFinalTranscriptUseCase useCase = new SaveFinalTranscriptUseCase(repository);

        SaveFinalTranscriptResult result =
                useCase.execute(
                        new SaveFinalTranscriptCommand(
                                eventId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "segment-1",
                                1L,
                                "ko",
                                "중복 저장 방지",
                                10L,
                                20L,
                                "openai-realtime-transcription",
                                "segment-1"));

        assertFalse(result.saved());
        assertEquals(0, repository.segments.size());
    }

    private static final class InMemoryTranscriptRepository
            implements MeetingTranscriptSegmentRepositoryPort {
        private final List<MeetingTranscriptSegment> segments = new ArrayList<>();
        private final List<UUID> eventIds = new ArrayList<>();

        @Override
        public MeetingTranscriptSegment save(MeetingTranscriptSegment segment) {
            segments.add(segment);
            eventIds.add(segment.sourceEventId());
            return segment;
        }

        @Override
        public boolean existsBySourceEventId(UUID sourceEventId) {
            return eventIds.contains(sourceEventId);
        }

        @Override
        public List<MeetingTranscriptSegment> findAllByMeetingIdOrderBySequence(UUID meetingId) {
            return segments.stream()
                    .filter(segment -> segment.meetingId().equals(meetingId))
                    .toList();
        }
    }
}
