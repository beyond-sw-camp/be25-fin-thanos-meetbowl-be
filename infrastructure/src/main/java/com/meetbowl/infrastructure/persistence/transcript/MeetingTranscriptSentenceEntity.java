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

/**
 * RabbitMQ로 수신한 최종 STT 원문을 문장 단위로 저장하는 JPA Entity다.
 *
 * <p>source_event_id는 이벤트 재전달 시 중복 저장을 막고, meeting_id와 sequence_no 조합은 회의 전체 원문을 정확한 순서로 재구성하기 위해
 * unique로 둔다.
 */
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

    /** 전체 원문 조회와 회의 소유권 판단에 사용하는 상위 회의 ID다. */
    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** 발화가 발생한 실시간 회의 진행 세션 ID다. 매핑 실패 가능성을 고려해 nullable이다. */
    @Column(name = "meeting_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID meetingSessionId;

    /** 발화자를 참가자 접속 이력과 연결할 수 있을 때 저장하는 세션 ID다. */
    @Column(name = "participant_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID participantSessionId;

    /** 회원 발화자의 사용자 ID다. 게스트 또는 미식별 발화자라면 null이다. */
    @Column(name = "speaker_user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID speakerUserId;

    /** 게스트 발화자의 권한 검증 식별자다. 회원 발화자라면 null이다. */
    @Column(name = "speaker_guest_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID speakerGuestSessionId;

    /** STT 공급자에서 전달한 speaker label로, 내부 참가자 매핑이 없을 때도 화자를 구분하는 보조값이다. */
    @Column(name = "speaker_label", length = 100)
    private String speakerLabel;

    /** 원문 문장의 인식 언어를 문자열 enum으로 저장해 enum 순서 변경 영향을 피한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscriptLanguage language;

    /** Deepgram final=true 이벤트에서 확정된 원본 문장 텍스트다. */
    @Column(name = "sentence_text", nullable = false, columnDefinition = "TEXT")
    private String sentenceText;

    /** 검색 또는 표시 보정용 텍스트다. 원본 문장을 보존하기 위해 별도 컬럼으로 둔다. */
    @Column(name = "normalized_text", columnDefinition = "TEXT")
    private String normalizedText;

    /** 발화 구간 시작 시각이다. 공급자 이벤트에 없으면 null일 수 있다. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** 발화 구간 종료 시각이다. 시작 시각과 함께 있을 때 Domain에서 순서를 검증한다. */
    @Column(name = "ended_at")
    private Instant endedAt;

    /** RabbitMQ 이벤트 ID로, 같은 최종 문장이 재전달될 때 중복 저장을 차단하는 멱등성 키다. */
    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID sourceEventId;

    /** 회의 내 문장 순서다. 비동기 저장 순서와 관계없이 전체 원문 정렬 기준으로 사용한다. */
    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    /** JPA 조회 전용 기본 생성자다. 도메인 검증 없이 직접 생성하지 않도록 protected로 제한한다. */
    protected MeetingTranscriptSentenceEntity() {}

    private MeetingTranscriptSentenceEntity(
            UUID meetingId,
            UUID meetingSessionId,
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
        this.meetingSessionId = meetingSessionId;
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

    /**
     * RabbitMQ에서 수신한 최종 문장 Domain Model을 저장용 Entity로 변환한다.
     *
     * <p>Domain에서 검증한 sourceEventId와 sequenceNo를 그대로 저장해 이벤트 멱등성과 원문 순서를 DB 제약으로도 보호한다.
     */
    static MeetingTranscriptSentenceEntity from(
            MeetingTranscriptSentence meetingTranscriptSentence) {
        MeetingTranscriptSentenceEntity entity =
                new MeetingTranscriptSentenceEntity(
                        meetingTranscriptSentence.meetingId(),
                        meetingTranscriptSentence.meetingSessionId(),
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

    /**
     * 저장된 Entity를 회의 원문 Domain Model로 복원한다.
     *
     * <p>DB 조회 결과도 Domain 팩토리를 통과시켜 빈 문장, 음수 순서, 상충하는 발화자 식별자 같은 문제를 다시 검증한다.
     */
    MeetingTranscriptSentence toDomain() {
        return MeetingTranscriptSentence.of(
                getId(),
                meetingId,
                meetingSessionId,
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
