package com.meetbowl.domain.transcript;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * Deepgram에서 final=true로 확정된 STT 결과를 문장 단위로 표현하는 도메인 모델이다.
 *
 * <p>실시간 자막의 중간 결과는 저장하지 않고, meetbowl-stt가 RabbitMQ로 전달한 최종 문장만 보관한다. 회의 전체 원문은 같은 meetingId의 문장을
 * sequenceNo 순서로 조회해 재구성한다.
 */
public class MeetingTranscriptSentence {

    /** 영속화된 원문 문장의 식별자이며 신규 이벤트 처리 시에는 null이다. */
    private final UUID id;

    /** 전체 원문을 조회하고 조립할 때 사용하는 상위 회의 식별자다. */
    private final UUID meetingId;

    /** Deepgram이 최종 확정한 한국어 원본 문장 텍스트다. 발화자 구분 정보는 저장하지 않는다. */
    private final String sentenceText;

    /** 해당 문장의 음성 구간 시작 시각이다. 공급자가 제공하지 않으면 null일 수 있다. */
    private final Instant startedAt;

    /** 해당 문장의 음성 구간 종료 시각이다. */
    private final Instant endedAt;

    /** RabbitMQ 이벤트의 고유 ID로, 재전달된 이벤트를 중복 저장하지 않기 위한 멱등성 키다. */
    private final UUID sourceEventId;

    /** 회의 내 문장 순서를 결정하는 번호로, 전체 원문 조회 시 오름차순 정렬 기준이 된다. */
    private final long sequenceNo;

    private MeetingTranscriptSentence(
            UUID id,
            UUID meetingId,
            String sentenceText,
            Instant startedAt,
            Instant endedAt,
            UUID sourceEventId,
            long sequenceNo) {
        this.id = id;
        this.meetingId = meetingId;
        this.sentenceText = sentenceText;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.sourceEventId = sourceEventId;
        this.sequenceNo = sequenceNo;
    }

    /**
     * STT 서버가 발행한 최종 문장 이벤트를 신규 도메인 모델로 생성한다.
     *
     * <p>이벤트 ID와 문장 순서를 그대로 보존해야 재시도 시 중복을 막고, 비동기 도착 순서와 무관하게 원문을 재구성할 수 있다.
     */
    public static MeetingTranscriptSentence create(
            UUID meetingId,
            String sentenceText,
            Instant startedAt,
            Instant endedAt,
            UUID sourceEventId,
            long sequenceNo) {
        return of(null, meetingId, sentenceText, startedAt, endedAt, sourceEventId, sequenceNo);
    }

    /**
     * 저장된 문장 또는 신규 이벤트 값을 복원하면서 원문 저장 규칙을 검증한다.
     *
     * <p>신규 생성과 DB 조회가 동일한 검증 경로를 사용하므로 빈 문장이나 잘못된 순서가 Domain 밖으로 전달되지 않는다.
     */
    public static MeetingTranscriptSentence of(
            UUID id,
            UUID meetingId,
            String sentenceText,
            Instant startedAt,
            Instant endedAt,
            UUID sourceEventId,
            long sequenceNo) {
        // 회의 ID는 원문 묶음의 소유자를, 이벤트 ID는 RabbitMQ 중복 소비 여부를 결정한다.
        require(meetingId, "회의 ID는 필수입니다.");
        require(sourceEventId, "원문 이벤트 ID는 필수입니다.");
        // final 이벤트라도 실제 문장이 비어 있다면 전체 원문에 의미 없는 행이 생기므로 저장하지 않는다.
        if (sentenceText == null || sentenceText.isBlank()) {
            throw invalid("회의 원문 문장은 필수입니다.");
        }
        // STT 서버가 부여한 순서 번호는 0부터 시작할 수 있지만 음수는 허용하지 않는다.
        if (sequenceNo < 0) {
            throw invalid("회의 원문 순서는 0 이상이어야 합니다.");
        }
        // 공급자가 두 시각을 모두 제공한 경우에만 음성 구간의 시간 순서를 검증한다.
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw invalid("발화 종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        return new MeetingTranscriptSentence(
                id, meetingId, sentenceText, startedAt, endedAt, sourceEventId, sequenceNo);
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

    public String sentenceText() {
        return sentenceText;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public UUID sourceEventId() {
        return sourceEventId;
    }

    public long sequenceNo() {
        return sequenceNo;
    }
}
