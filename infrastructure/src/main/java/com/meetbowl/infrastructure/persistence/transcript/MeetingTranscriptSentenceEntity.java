package com.meetbowl.infrastructure.persistence.transcript;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.transcript.MeetingTranscriptSentence;
import com.meetbowl.domain.transcript.TranscriptLanguage;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** RabbitMQ로 수신한 최종 STT 원문을 문장 단위로 저장하는 JPA Entity다. */
@Entity
@Table(
        name = "meeting_transcript_sentences",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_transcript_sentences_meeting_sequence",
                    columnNames = {"meeting_id", "sequence_no"}),
            @UniqueConstraint(
                    name = "uk_transcript_sentences_source_event",
                    columnNames = "source_event_id")
        },
        indexes = {
            @Index(
                    name = "idx_transcript_sentences_meeting_started",
                    columnList = "meeting_id, started_at")
        })
public class MeetingTranscriptSentenceEntity extends BaseEntity {

    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Column(name = "video_room_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID videoRoomId;

    @Column(name = "participant_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID participantSessionId;

    @Column(name = "speaker_user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID speakerUserId;

    @Column(name = "speaker_guest_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID speakerGuestSessionId;

    @Column(name = "speaker_label", length = 100)
    private String speakerLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscriptLanguage language;

    @Column(name = "sentence_text", nullable = false, columnDefinition = "TEXT")
    private String sentenceText;

    @Column(name = "normalized_text", columnDefinition = "TEXT")
    private String normalizedText;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID sourceEventId;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    protected MeetingTranscriptSentenceEntity() {}

    private MeetingTranscriptSentenceEntity(
            UUID meetingId,
            UUID videoRoomId,
            UUID participantSessionId,
            UUID speakerUserId,
            UUID speakerGuestSessionId,
            String speakerLabel,
            TranscriptLanguage language,
            String sentenceText,
            String normalizedText,
            Instant startedAt,
            Instant endedAt,
            UUID sourceEventId,
            long sequenceNo) {
        this.meetingId = meetingId;
        this.videoRoomId = videoRoomId;
        this.participantSessionId = participantSessionId;
        this.speakerUserId = speakerUserId;
        this.speakerGuestSessionId = speakerGuestSessionId;
        this.speakerLabel = speakerLabel;
        this.language = language;
        this.sentenceText = sentenceText;
        this.normalizedText = normalizedText;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.sourceEventId = sourceEventId;
        this.sequenceNo = sequenceNo;
    }

    static MeetingTranscriptSentenceEntity from(
            MeetingTranscriptSentence meetingTranscriptSentence) {
        MeetingTranscriptSentenceEntity entity =
                new MeetingTranscriptSentenceEntity(
                        meetingTranscriptSentence.meetingId(),
                        meetingTranscriptSentence.videoRoomId(),
                        meetingTranscriptSentence.participantSessionId(),
                        meetingTranscriptSentence.speakerUserId(),
                        meetingTranscriptSentence.speakerGuestSessionId(),
                        meetingTranscriptSentence.speakerLabel(),
                        meetingTranscriptSentence.language(),
                        meetingTranscriptSentence.sentenceText(),
                        meetingTranscriptSentence.normalizedText(),
                        meetingTranscriptSentence.startedAt(),
                        meetingTranscriptSentence.endedAt(),
                        meetingTranscriptSentence.sourceEventId(),
                        meetingTranscriptSentence.sequenceNo());
        entity.setId(meetingTranscriptSentence.id());
        return entity;
    }

    MeetingTranscriptSentence toDomain() {
        return MeetingTranscriptSentence.of(
                getId(),
                meetingId,
                videoRoomId,
                participantSessionId,
                speakerUserId,
                speakerGuestSessionId,
                speakerLabel,
                language,
                sentenceText,
                normalizedText,
                startedAt,
                endedAt,
                sourceEventId,
                sequenceNo);
    }
}
