package com.meetbowl.domain.transcript;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의 중 확정된 최종 발화 segment 하나를 표현하는 도메인 모델이다.
 *
 * <p>OpenAI Realtime Translation은 번역 결과를 delta로 계속 흘려보내므로, DB에는 중간 조각을 저장하지 않고 STT 서버가 발화 종료를 확정한
 * FINAL segment만 저장한다. 회의 전체 원문은 같은 meetingId의 segment를 sequence 기준으로 정렬해 sourceText를 이어 붙여 재구성한다.
 */
public class MeetingTranscriptSegment {

    /** 영속화된 회의 원문 segment 식별자이며 신규 이벤트 처리 시에는 null이다. */
    private final UUID id;

    /** 전체 원문과 번역 묶음의 소유 회의를 식별한다. */
    private final UUID meetingId;

    /** STT 서버가 현재 발화 버퍼를 확정할 때 생성한 segment 식별자다. 회의 내부에서 중복되면 안 된다. */
    private final String segmentId;

    /** 최종 회의 원문 조립과 화면 재구성 시 사용하는 회의 내 segment 순번이다. */
    private final long sequence;

    /** 원문이 어떤 언어 발화에서 시작됐는지 나타낸다. 현재는 한국어/영어만 허용한다. */
    private final TranscriptLanguage sourceLanguage;

    /** 발화가 시작된 원문 텍스트다. 최종 회의 원문은 이 값을 sequence 순서대로 합쳐서 만든다. */
    private final String sourceText;

    /** 한국어 탭 표시와 회의록 후처리에 사용하는 한국어 최종 텍스트다. */
    private final String koText;

    /** 영어 탭 표시와 회의록 후처리에 사용하는 영어 최종 텍스트다. */
    private final String enText;

    /** 회의 시작 기준 발화 시작 오프셋(ms)이다. */
    private final Long startedAtMs;

    /** 회의 시작 기준 발화 종료 오프셋(ms)이다. */
    private final Long endedAtMs;

    /** segment 저장 상태다. 현재 MariaDB에는 FINAL 상태만 저장한다. */
    private final MeetingTranscriptSegmentStatus status;

    /** RabbitMQ 이벤트의 고유 ID로, 재전달된 이벤트를 중복 저장하지 않기 위한 멱등성 키다. */
    private final UUID sourceEventId;

    private MeetingTranscriptSegment(
            UUID id,
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
        this.id = id;
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
     * STT 서버가 발화 종료를 확정한 최종 segment를 신규 도메인 모델로 생성한다.
     *
     * <p>API 서버는 RabbitMQ에서 받은 최종 segment만 저장하므로 status는 항상 FINAL로 고정한다. 중간 delta와 버퍼 상태는 STT 서버나
     * 임시 캐시에만 남긴다.
     */
    public static MeetingTranscriptSegment createFinal(
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
        return of(
                null,
                meetingId,
                segmentId,
                sequence,
                sourceLanguage,
                sourceText,
                koText,
                enText,
                startedAtMs,
                endedAtMs,
                MeetingTranscriptSegmentStatus.FINAL,
                sourceEventId);
    }

    /**
     * 저장된 segment 또는 신규 이벤트 값을 복원하면서 최종 저장 규칙을 검증한다.
     *
     * <p>현재 영속화 모델은 FINAL segment 전용이므로 STREAMING 상태는 허용하지 않는다. 이렇게 해야 회의록과 전체 원문 조회 결과에 미완성 delta가
     * 섞이지 않는다.
     */
    public static MeetingTranscriptSegment of(
            UUID id,
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
        require(meetingId, "회의 ID는 필수입니다.");
        require(sourceEventId, "원문 이벤트 ID는 필수입니다.");
        require(sourceLanguage, "원문 언어는 필수입니다.");
        require(status, "원문 segment 상태는 필수입니다.");
        if (segmentId == null || segmentId.isBlank()) {
            throw invalid("원문 segment ID는 필수입니다.");
        }
        if (sourceText == null || sourceText.isBlank()) {
            throw invalid("원문 텍스트는 필수입니다.");
        }
        if (koText == null || koText.isBlank()) {
            throw invalid("한국어 원문 텍스트는 필수입니다.");
        }
        if (enText == null || enText.isBlank()) {
            throw invalid("영어 원문 텍스트는 필수입니다.");
        }
        if (sequence < 0) {
            throw invalid("회의 원문 순서는 0 이상이어야 합니다.");
        }
        if (startedAtMs != null && startedAtMs < 0) {
            throw invalid("발화 시작 시각(ms)은 0 이상이어야 합니다.");
        }
        if (endedAtMs != null && endedAtMs < 0) {
            throw invalid("발화 종료 시각(ms)은 0 이상이어야 합니다.");
        }
        if (startedAtMs != null && endedAtMs != null && endedAtMs < startedAtMs) {
            throw invalid("발화 종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        if (status != MeetingTranscriptSegmentStatus.FINAL) {
            throw invalid("DB 저장 대상 원문 segment 상태는 FINAL만 허용합니다.");
        }
        return new MeetingTranscriptSegment(
                id,
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

    private static void require(Object value, String message) {
        if (value == null) {
            throw invalid(message);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public String segmentId() {
        return segmentId;
    }

    public long sequence() {
        return sequence;
    }

    public TranscriptLanguage sourceLanguage() {
        return sourceLanguage;
    }

    public String sourceText() {
        return sourceText;
    }

    public String koText() {
        return koText;
    }

    public String enText() {
        return enText;
    }

    public Long startedAtMs() {
        return startedAtMs;
    }

    public Long endedAtMs() {
        return endedAtMs;
    }

    public MeetingTranscriptSegmentStatus status() {
        return status;
    }

    public UUID sourceEventId() {
        return sourceEventId;
    }
}
