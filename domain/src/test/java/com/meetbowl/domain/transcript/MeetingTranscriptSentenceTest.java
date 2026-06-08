package com.meetbowl.domain.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class MeetingTranscriptSentenceTest {

    @Test
    void createFinalTranscriptSentence() {
        UUID meetingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        MeetingTranscriptSentence sentence =
                MeetingTranscriptSentence.create(
                        meetingId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "speaker-0",
                        TranscriptLanguage.KO,
                        "회의를 시작하겠습니다.",
                        null,
                        Instant.parse("2099-01-01T00:00:01Z"),
                        Instant.parse("2099-01-01T00:00:03Z"),
                        eventId,
                        0);

        assertEquals(meetingId, sentence.meetingId());
        assertEquals(eventId, sentence.sourceEventId());
        assertEquals("회의를 시작하겠습니다.", sentence.sentenceText());
        assertEquals(0, sentence.sequenceNo());
    }

    @Test
    void sequenceNumberMustNotBeNegative() {
        assertThrows(
                BusinessException.class,
                () ->
                        MeetingTranscriptSentence.create(
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                TranscriptLanguage.KO,
                                "문장",
                                null,
                                null,
                                null,
                                UUID.randomUUID(),
                                -1));
    }
}
