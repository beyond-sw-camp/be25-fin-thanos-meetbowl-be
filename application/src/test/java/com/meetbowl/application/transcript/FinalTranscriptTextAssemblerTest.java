package com.meetbowl.application.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.TranscriptLanguage;

class FinalTranscriptTextAssemblerTest {

    @Test
    void sortsBySequenceAndNormalizesWhitespace() {
        UUID meetingId = UUID.randomUUID();
        FinalTranscriptTextAssembler assembler = new FinalTranscriptTextAssembler();

        String result =
                assembler.assemble(
                        List.of(
                                segment(meetingId, "second", 2, "  둘째 문장  "),
                                segment(meetingId, "first", 1, "첫 문장")));

        assertEquals("첫 문장\n둘째 문장", result);
    }

    private MeetingTranscriptSegment segment(
            UUID meetingId, String segmentId, long sequence, String text) {
        return MeetingTranscriptSegment.create(
                meetingId,
                segmentId,
                sequence,
                TranscriptLanguage.KO,
                text,
                text,
                text,
                0L,
                1L,
                UUID.randomUUID());
    }
}
