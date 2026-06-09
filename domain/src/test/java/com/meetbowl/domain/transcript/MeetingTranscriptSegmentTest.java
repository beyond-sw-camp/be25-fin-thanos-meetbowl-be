package com.meetbowl.domain.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class MeetingTranscriptSegmentTest {

    @Test
    void createTranscriptSegment() {
        UUID meetingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        MeetingTranscriptSegment segment =
                MeetingTranscriptSegment.create(
                        meetingId,
                        "segment-001",
                        0,
                        TranscriptLanguage.KO,
                        "회의를 시작하겠습니다.",
                        "회의를 시작하겠습니다.",
                        "Let's begin the meeting.",
                        1000L,
                        3000L,
                        eventId);

        assertEquals(meetingId, segment.meetingId());
        assertEquals("segment-001", segment.segmentId());
        assertEquals(eventId, segment.sourceEventId());
        assertEquals("회의를 시작하겠습니다.", segment.sourceText());
        assertEquals("회의를 시작하겠습니다.", segment.koText());
        assertEquals("Let's begin the meeting.", segment.enText());
        assertEquals(0, segment.sequence());
    }

    @Test
    void sequenceMustNotBeNegative() {
        assertThrows(
                BusinessException.class,
                () ->
                        MeetingTranscriptSegment.create(
                                UUID.randomUUID(),
                                "segment-001",
                                -1,
                                TranscriptLanguage.KO,
                                "문장",
                                "문장",
                                "sentence",
                                null,
                                null,
                                UUID.randomUUID()));
    }
}
