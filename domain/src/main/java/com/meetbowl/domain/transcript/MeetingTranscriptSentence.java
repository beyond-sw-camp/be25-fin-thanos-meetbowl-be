package com.meetbowl.domain.transcript;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** Deepgram의 최종 인식 결과를 문장 단위로 저장하는 회의 원문 모델이다. */
public class MeetingTranscriptSentence {

    private final UUID id;
    private final UUID meetingId;
    private final UUID videoRoomId;
    private final UUID participantSessionId;
    private final UUID speakerUserId;
    private final UUID speakerGuestSessionId;
    private final String speakerLabel;
    private final TranscriptLanguage language;
    private final String sentenceText;
    private final String normalizedText;
    private final Instant startedAt;
    private final Instant endedAt;
    private final UUID sourceEventId;
    private final long sequenceNo;

    private MeetingTranscriptSentence(
            UUID id,
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
        this.id = id;
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

    public static MeetingTranscriptSentence create(
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
        return of(
                null,
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

    public static MeetingTranscriptSentence of(
            UUID id,
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
        require(meetingId, "회의 ID는 필수입니다.");
        require(language, "회의 원문 언어는 필수입니다.");
        require(sourceEventId, "원문 이벤트 ID는 필수입니다.");
        if (sentenceText == null || sentenceText.isBlank()) {
            throw invalid("회의 원문 문장은 필수입니다.");
        }
        if (sequenceNo < 0) {
            throw invalid("회의 원문 순서는 0 이상이어야 합니다.");
        }
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw invalid("발화 종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        if (speakerUserId != null && speakerGuestSessionId != null) {
            throw invalid("회원 발화자와 게스트 발화자를 동시에 지정할 수 없습니다.");
        }
        return new MeetingTranscriptSentence(
                id,
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

    public UUID videoRoomId() {
        return videoRoomId;
    }

    public UUID participantSessionId() {
        return participantSessionId;
    }

    public UUID speakerUserId() {
        return speakerUserId;
    }

    public UUID speakerGuestSessionId() {
        return speakerGuestSessionId;
    }

    public String speakerLabel() {
        return speakerLabel;
    }

    public TranscriptLanguage language() {
        return language;
    }

    public String sentenceText() {
        return sentenceText;
    }

    public String normalizedText() {
        return normalizedText;
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
