package com.meetbowl.domain.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FinalTranscriptTextAssemblerTest {

    @Test
    void sequence순서대로공백없는문장만합친다() {
        var meetingId = UUID.randomUUID();

        String result =
                FinalTranscriptTextAssembler.assemble(
                        List.of(
                                MeetingTranscriptSegment.create(
                                        meetingId,
                                        "segment-2",
                                        2L,
                                        TranscriptLanguage.KO,
                                        " 둘째 문장 ",
                                        "둘째 문장",
                                        "둘째 문장",
                                        100L,
                                        200L,
                                        UUID.randomUUID()),
                                MeetingTranscriptSegment.create(
                                        meetingId,
                                        "segment-1",
                                        1L,
                                        TranscriptLanguage.KO,
                                        "첫 문장",
                                        "첫 문장",
                                        "첫 문장",
                                        0L,
                                        100L,
                                        UUID.randomUUID())));

        assertEquals("첫 문장\n둘째 문장", result);
    }
}
