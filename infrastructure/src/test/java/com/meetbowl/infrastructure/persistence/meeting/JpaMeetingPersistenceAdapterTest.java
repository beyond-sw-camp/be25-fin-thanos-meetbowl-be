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

import com.meetbowl.domain.transcript.MeetingTranscriptSentence;
import com.meetbowl.domain.transcript.MeetingTranscriptSentenceRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.transcript.JpaMeetingTranscriptSentenceRepositoryAdapter;

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
    private MeetingTranscriptSentenceRepositoryPort meetingTranscriptSentenceRepositoryPort;

    @Test
    void findTranscriptSentencesInSequenceAndCheckEventDuplication() {
        UUID meetingId = UUID.randomUUID();
        UUID firstEventId = UUID.randomUUID();
        MeetingTranscriptSentence second =
                createSentence(meetingId, UUID.randomUUID(), 2, "두 번째 문장입니다.");
        MeetingTranscriptSentence first = createSentence(meetingId, firstEventId, 1, "첫 번째 문장입니다.");

        meetingTranscriptSentenceRepositoryPort.save(second);
        meetingTranscriptSentenceRepositoryPort.save(first);

        List<MeetingTranscriptSentence> sentences =
                meetingTranscriptSentenceRepositoryPort.findAllByMeetingIdOrderBySequenceNo(
                        meetingId);

        assertThat(sentences)
                .extracting(MeetingTranscriptSentence::sequenceNo)
                .containsExactly(1L, 2L);
        assertThat(meetingTranscriptSentenceRepositoryPort.existsBySourceEventId(firstEventId))
                .isTrue();
    }

    private MeetingTranscriptSentence createSentence(
            UUID meetingId, UUID sourceEventId, long sequenceNo, String sentenceText) {
        return MeetingTranscriptSentence.create(
                meetingId,
                sentenceText,
                Instant.parse("2099-01-01T00:00:01Z").plusSeconds(sequenceNo),
                Instant.parse("2099-01-01T00:00:02Z").plusSeconds(sequenceNo),
                sourceEventId,
                sequenceNo);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        MeetingPersistenceJpaConfig.class,
        JpaMeetingTranscriptSentenceRepositoryAdapter.class
    })
    static class TestApplication {}
}
