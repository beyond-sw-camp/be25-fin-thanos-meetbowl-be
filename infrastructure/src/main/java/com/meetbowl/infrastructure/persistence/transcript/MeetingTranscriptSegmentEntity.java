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
import com.meetbowl.domain.transcript.TranscriptLanguage;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회의 발화 세그먼트를 저장하기 위한 JPA 엔티티 클래스입니다.
 *
 * <p>이 엔티티는 "실시간 자막 전체"를 저장하지 않는다.
 * STT 서버가 FINALIZED로 확정한 세그먼트만 한 건씩 저장한다.
 *
 * <p>즉, 이 테이블은
 * - partial/interim caption 로그 저장소가 아니라
 * - 최종 원문 재조립을 위한 finalized segment 저장소다.
 *
 * <p>STT 서버로부터 수신된 최종 확정(Finalized) 데이터를 'meeting_transcript_segments' 테이블에 매핑합니다.
 */
@Entity
@Table(
        name = "meeting_transcript_segments",
        uniqueConstraints = {
            /** 1. 회의별 세그먼트 ID 고유성 보장 */
            @UniqueConstraint(
                    name = "uk_transcript_segments_meeting_segment",
                    columnNames = {"meeting_id", "segment_id"}),
            /** 2. 회의별 발화 순서 중복 방지 */
            @UniqueConstraint(
                    name = "uk_transcript_segments_meeting_sequence",
                    columnNames = {"meeting_id", "sequence_no"}),
            /** 3. 메시지 큐 이벤트의 중복 처리(Idempotency)를 위한 고유 키 */
            @UniqueConstraint(
                    name = "uk_transcript_segments_source_event",
                    columnNames = "source_event_id")
        },
        indexes = {
            /** 조회 성능 최적화: 회의별로 순차적인 자막 조회를 빈번하게 수행하므로 복합 인덱스 구성 */
            @Index(
                    name = "idx_transcript_segments_meeting_sequence",
                    columnList = "meeting_id, sequence_no")
        })
public class MeetingTranscriptSegmentEntity extends BaseEntity {

    /** 자막이 속한 상위 회의의 식별자입니다. */
    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** STT 엔진에서 생성한 세그먼트 자체의 식별자입니다. 같은 회의 안에서만 유일하면 충분합니다. */
    @Column(name = "segment_id", nullable = false, updatable = false, length = 100)
    private String segmentId;

    /** 회의 전체 맥락에서 이 세그먼트가 차지하는 순서입니다. 원문 재조립과 화면 순서의 기준이 됩니다. */
    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequence;

    /** 발화가 시작된 원본 언어 정보를 저장합니다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_language", nullable = false, updatable = false, length = 10)
    private TranscriptLanguage sourceLanguage;

    /** STT finalized 결과의 원문 텍스트입니다. partial/interim은 여기에 저장되지 않습니다. */
    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    /** 사용자 인터페이스의 한국어 탭에서 표시될 가공된 텍스트입니다. */
    @Column(name = "ko_text", nullable = false, columnDefinition = "TEXT")
    private String koText;

    /** 사용자 인터페이스의 영어 탭에서 표시될 가공된 텍스트입니다. */
    @Column(name = "en_text", nullable = false, columnDefinition = "TEXT")
    private String enText;

    /** 회의 시작 시점 대비 발화 시작 오프셋(ms)입니다. */
    @Column(name = "started_at_ms")
    private Long startedAtMs;

    /** 회의 시작 시점 대비 발화 종료 오프셋(ms)입니다. */
    @Column(name = "ended_at_ms")
    private Long endedAtMs;

    /**
     * RabbitMQ 메시지의 eventId입니다.
     *
     * 역할:
     * - STT가 한 finalized 문장을 이벤트로 발행했을 때 그 이벤트 자체를 식별한다.
     * - 네트워크 재시도 등으로 동일 메시지가 다시 와도, DB unique constraint로 중복 저장을 막는다.
     *
     * 중요한 구분:
     * - segmentId: "문장 세그먼트" 식별자
     * - sourceEventId: "브로커를 통해 들어온 이벤트 envelope" 식별자
     */
    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID sourceEventId;

    /** JPA 프록시 생성을 위한 기본 생성자입니다. */
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
        this.sourceEventId = sourceEventId;
    }

    /** 도메인 모델을 영속성 엔티티로 변환합니다. */
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
                        meetingTranscriptSegment.sourceEventId());
        entity.setId(meetingTranscriptSegment.id());
        return entity;
    }

    /** DB에서 조회된 영속성 엔티티를 비즈니스 로직 처리를 위한 도메인 모델로 변환합니다. */
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
                sourceEventId);
    }
}
