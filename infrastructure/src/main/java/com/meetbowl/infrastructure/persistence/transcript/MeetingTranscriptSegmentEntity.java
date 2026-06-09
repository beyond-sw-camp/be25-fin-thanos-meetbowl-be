package com.meetbowl.infrastructure.persistence.transcript;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentStatus;
import com.meetbowl.domain.transcript.TranscriptLanguage;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * RabbitMQ로 수신한 최종 회의 원문 segment를 저장하는 JPA Entity다. OpenAI Realtime Translation의 delta 조각은 저장하지 않고,
 * STT 서버가 확정한 FINAL segment만 저장한다. 회의 전체 원문과 양방향 번역문을 함께 조회할 수 있도록 source/ko/en 텍스트를 한 행에 보관한다.
 */
@Entity
@Table(
        name = "meeting_transcript_segments",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_transcript_segments_meeting_segment",
                    columnNames = {"meeting_id", "segment_id"}),
            @UniqueConstraint(
                    name = "uk_transcript_segments_meeting_sequence",
                    columnNames = {"meeting_id", "sequence_no"}),
            @UniqueConstraint(
                    name = "uk_transcript_segments_source_event",
                    columnNames = "source_event_id")
        },
        indexes = {
            @Index(
                    name = "idx_transcript_segments_meeting_sequence",
                    columnList = "meeting_id, sequence_no")
        })
public class MeetingTranscriptSegmentEntity extends BaseEntity {

    /** 전체 원문 조회와 회의 소유권 판단에 사용하는 상위 회의 ID다. */
    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** STT 서버가 발화 종료 시 생성한 segment ID다. 동일 회의 안에서 중복되면 안 된다. */
    @Column(name = "segment_id", nullable = false, updatable = false, length = 100)
    private String segmentId;

    /** 회의 내 발화 segment의 최종 순서다. 전체 원문 조립 시 이 값을 기준으로 정렬한다. */
    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequence;

    /** 원문 발화가 시작된 언어다. 현재 실시간 번역 구조상 KO와 EN만 허용한다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_language", nullable = false, updatable = false, length = 10)
    private TranscriptLanguage sourceLanguage;

    /** 최종 회의 원문으로 합칠 원본 발화 텍스트다. */
    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    /** 한국어 탭 표시와 회의록 후처리에 사용할 최종 한국어 텍스트다. */
    @Column(name = "ko_text", nullable = false, columnDefinition = "TEXT")
    private String koText;

    /** 영어 탭 표시와 회의록 후처리에 사용할 최종 영어 텍스트다. */
    @Column(name = "en_text", nullable = false, columnDefinition = "TEXT")
    private String enText;

    /** 회의 시작 시점 기준 발화 시작 오프셋(ms)이다. */
    @Column(name = "started_at_ms")
    private Long startedAtMs;

    /** 회의 시작 시점 기준 발화 종료 오프셋(ms)이다. */
    @Column(name = "ended_at_ms")
    private Long endedAtMs;

    /** 현재 영속화 정책상 항상 FINAL이지만, 상태 필드를 남겨 런타임 모델과 저장 모델의 의미를 맞춘다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeetingTranscriptSegmentStatus status;

    /** RabbitMQ 이벤트 ID로, 같은 최종 segment가 재전달될 때 중복 저장을 차단하는 멱등성 키다. */
    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID sourceEventId;

    /** JPA 조회 전용 기본 생성자다. 도메인 검증 없이 직접 생성하지 않도록 protected로 제한한다. */
    protected MeetingTranscriptSegmentEntity() {}

    private MeetingTranscriptSegmentEntity(
            UUID meetingId,
            String segmentId,
            long sequence,
            TranscriptLanguage sourceLanguage,
            String sourceText,
            String koText,
            String enText,
            Long startedAtMs,
            Long endedAtMs,
            MeetingTranscriptSegmentStatus status,
            UUID sourceEventId) {
        this.meetingId = meetingId;
        this.segmentId = segmentId;
        this.sequence = sequence;
        this.sourceLanguage = sourceLanguage;
        this.sourceText = sourceText;
        this.koText = koText;
        this.enText = enText;
        this.startedAtMs = startedAtMs;
        this.endedAtMs = endedAtMs;
        this.status = status;
        this.sourceEventId = sourceEventId;
    }

    /**
     * 최종 발화 segment 도메인 모델을 저장용 Entity로 변환한다.
     *
     * <p>sourceEventId와 meetingId+segmentId/sequence 제약을 함께 저장해 재전달 메시지 중복과 순서 충돌을 DB 수준에서도 막는다.
     */
    static MeetingTranscriptSegmentEntity from(MeetingTranscriptSegment meetingTranscriptSegment) {
        MeetingTranscriptSegmentEntity entity =
                new MeetingTranscriptSegmentEntity(
                        meetingTranscriptSegment.meetingId(),
                        meetingTranscriptSegment.segmentId(),
                        meetingTranscriptSegment.sequence(),
                        meetingTranscriptSegment.sourceLanguage(),
                        meetingTranscriptSegment.sourceText(),
                        meetingTranscriptSegment.koText(),
                        meetingTranscriptSegment.enText(),
                        meetingTranscriptSegment.startedAtMs(),
                        meetingTranscriptSegment.endedAtMs(),
                        meetingTranscriptSegment.status(),
                        meetingTranscriptSegment.sourceEventId());
        entity.setId(meetingTranscriptSegment.id());
        return entity;
    }

    /**
     * 저장된 Entity를 최종 회의 원문 segment 도메인 모델로 복원한다.
     *
     * <p>DB 조회 결과도 Domain 팩토리를 통과시켜 빈 텍스트, 잘못된 상태, 음수 시간 같은 문제를 다시 검증한다.
     */
    MeetingTranscriptSegment toDomain() {
        return MeetingTranscriptSegment.of(
                getId(),
                meetingId,
                segmentId,
                sequence,
                sourceLanguage,
                sourceText,
                koText,
                enText,
                startedAtMs,
                endedAtMs,
                status,
                sourceEventId);
    }
}
