package com.meetbowl.infrastructure.persistence.meeting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;
import com.meetbowl.domain.transcript.TranscriptLanguage;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.transcript.JpaMeetingTranscriptSegmentRepositoryAdapter;

@SpringBootTest(classes = JpaMeetingPersistenceAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:meeting-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaMeetingPersistenceAdapterTest {

    @Autowired
    private MeetingTranscriptSegmentRepositoryPort meetingTranscriptSegmentRepositoryPort;

    @Test
    void findTranscriptSegmentsInSequenceAndCheckEventDuplication() {
        UUID meetingId = UUID.randomUUID();
        UUID firstEventId = UUID.randomUUID();
        MeetingTranscriptSegment second =
                createSegment(meetingId, UUID.randomUUID(), "segment-002", 2, "두 번째 문장입니다.");
        MeetingTranscriptSegment first =
                createSegment(meetingId, firstEventId, "segment-001", 1, "첫 번째 문장입니다.");

        meetingTranscriptSegmentRepositoryPort.save(second);
        meetingTranscriptSegmentRepositoryPort.save(first);

        List<MeetingTranscriptSegment> segments =
                meetingTranscriptSegmentRepositoryPort.findAllByMeetingIdOrderBySequence(meetingId);

        assertThat(segments).extracting(MeetingTranscriptSegment::sequence).containsExactly(1L, 2L);
        assertThat(meetingTranscriptSegmentRepositoryPort.existsBySourceEventId(firstEventId))
                .isTrue();
    }

    private MeetingTranscriptSegment createSegment(
            UUID meetingId,
            UUID sourceEventId,
            String segmentId,
            long sequenceNo,
            String sourceText) {
        return MeetingTranscriptSegment.createFinal(
                meetingId,
                segmentId,
                sequenceNo,
                TranscriptLanguage.KO,
                sourceText,
                sourceText,
                "translated-" + sequenceNo,
                Instant.parse("2099-01-01T00:00:01Z").plusSeconds(sequenceNo).toEpochMilli(),
                Instant.parse("2099-01-01T00:00:02Z").plusSeconds(sequenceNo).toEpochMilli(),
                sourceEventId);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        MeetingPersistenceJpaConfig.class,
        JpaMeetingTranscriptSegmentRepositoryAdapter.class
    })
    static class TestApplication {}
}
