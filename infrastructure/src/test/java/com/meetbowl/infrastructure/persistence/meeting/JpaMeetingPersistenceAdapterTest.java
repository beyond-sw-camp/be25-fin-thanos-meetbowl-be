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

import com.meetbowl.domain.meeting.MeetingProvider;
import com.meetbowl.domain.meeting.MeetingSession;
import com.meetbowl.domain.meeting.MeetingSessionRepositoryPort;
import com.meetbowl.domain.meeting.ParticipantSession;
import com.meetbowl.domain.meeting.ParticipantSessionRepositoryPort;
import com.meetbowl.domain.meeting.ParticipantSessionStatus;
import com.meetbowl.domain.meeting.ParticipantType;
import com.meetbowl.domain.transcript.MeetingTranscriptSentence;
import com.meetbowl.domain.transcript.MeetingTranscriptSentenceRepositoryPort;
import com.meetbowl.domain.transcript.TranscriptLanguage;
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

    @Autowired private MeetingSessionRepositoryPort meetingSessionRepositoryPort;
    @Autowired private ParticipantSessionRepositoryPort participantSessionRepositoryPort;

    @Autowired
    private MeetingTranscriptSentenceRepositoryPort meetingTranscriptSentenceRepositoryPort;

    @Test
    void saveAndFindMeetingSessionByMeetingId() {
        UUID meetingId = UUID.randomUUID();
        MeetingSession meetingSession =
                MeetingSession.create(
                        meetingId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MeetingProvider.LIVEKIT,
                        "meeting-" + meetingId);

        MeetingSession saved = meetingSessionRepositoryPort.save(meetingSession);

        assertThat(saved.id()).isNotNull();
        assertThat(meetingSessionRepositoryPort.findByMeetingId(meetingId))
                .get()
                .extracting(MeetingSession::id)
                .isEqualTo(saved.id());
    }

    @Test
    void findOnlyJoinedParticipantSessions() {
        UUID meetingSessionId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        ParticipantSession joined =
                ParticipantSession.of(
                        null,
                        meetingSessionId,
                        meetingId,
                        ParticipantType.PARTICIPANT,
                        UUID.randomUUID(),
                        null,
                        "참가자",
                        "joined-user",
                        ParticipantSessionStatus.JOINED,
                        Instant.parse("2099-01-01T00:01:00Z"),
                        Instant.parse("2099-01-01T00:01:00Z"));
        ParticipantSession requested =
                ParticipantSession.createMember(
                        meetingSessionId,
                        meetingId,
                        ParticipantType.PARTICIPANT,
                        UUID.randomUUID(),
                        "입장 대기자",
                        "requested-user");

        ParticipantSession savedJoined = participantSessionRepositoryPort.save(joined);
        participantSessionRepositoryPort.save(requested);

        assertThat(participantSessionRepositoryPort.findJoinedByMeetingSessionId(meetingSessionId))
                .extracting(ParticipantSession::id)
                .containsExactly(savedJoined.id());
    }

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
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "speaker-0",
                TranscriptLanguage.KO,
                sentenceText,
                null,
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
        JpaMeetingSessionRepositoryAdapter.class,
        JpaParticipantSessionRepositoryAdapter.class,
        JpaMeetingTranscriptSentenceRepositoryAdapter.class
    })
    static class TestApplication {}
}
