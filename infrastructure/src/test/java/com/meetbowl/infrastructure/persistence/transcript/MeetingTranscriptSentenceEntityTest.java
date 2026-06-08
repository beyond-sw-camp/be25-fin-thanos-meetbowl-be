package com.meetbowl.infrastructure.persistence.transcript;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.transcript.MeetingTranscriptSentence;
import com.meetbowl.domain.transcript.TranscriptLanguage;

class MeetingTranscriptSentenceEntityTest {

    @Test
    void transcriptSentenceRoundTrip() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        MeetingTranscriptSentence source =
                MeetingTranscriptSentence.of(
                        id,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "speaker-0",
                        TranscriptLanguage.KO,
                        "회의를 시작하겠습니다.",
                        "회의를 시작하겠습니다.",
                        Instant.parse("2099-01-01T00:00:01Z"),
                        Instant.parse("2099-01-01T00:00:03Z"),
                        eventId,
                        1);

        MeetingTranscriptSentence restored =
                MeetingTranscriptSentenceEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.sourceEventId()).isEqualTo(eventId);
        assertThat(restored.sequenceNo()).isEqualTo(1);
        assertThat(restored.sentenceText()).isEqualTo("회의를 시작하겠습니다.");
    }
}
