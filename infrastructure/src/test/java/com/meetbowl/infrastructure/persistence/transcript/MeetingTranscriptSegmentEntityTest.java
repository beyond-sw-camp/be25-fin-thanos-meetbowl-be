package com.meetbowl.infrastructure.persistence.transcript;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.TranscriptLanguage;

class MeetingTranscriptSegmentEntityTest {

    @Test
    void transcriptSegmentRoundTrip() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        MeetingTranscriptSegment source =
                MeetingTranscriptSegment.of(
                        id,
                        UUID.randomUUID(),
                        "segment-001",
                        1,
                        TranscriptLanguage.EN,
                        "Let's start the meeting.",
                        "회의를 시작합시다.",
                        "Let's start the meeting.",
                        1000L,
                        3000L,
                        eventId);

        MeetingTranscriptSegment restored = MeetingTranscriptSegmentEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.segmentId()).isEqualTo("segment-001");
        assertThat(restored.sourceEventId()).isEqualTo(eventId);
        assertThat(restored.sequence()).isEqualTo(1);
        assertThat(restored.sourceLanguage()).isEqualTo(TranscriptLanguage.EN);
        assertThat(restored.koText()).isEqualTo("회의를 시작합시다.");
        assertThat(restored.enText()).isEqualTo("Let's start the meeting.");
    }
}
