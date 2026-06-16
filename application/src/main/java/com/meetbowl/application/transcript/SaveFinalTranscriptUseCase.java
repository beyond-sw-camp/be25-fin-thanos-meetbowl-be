package com.meetbowl.application.transcript;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;
import com.meetbowl.domain.transcript.TranscriptLanguage;

/**
 * STT 서버가 RabbitMQ로 보낸 최종 발화 segment를 MariaDB에 멱등하게 저장한다.
 *
 * <p>중간 delta는 저장하지 않고 `FINALIZED`만 저장하며, 같은 `sourceEventId`가 다시 들어와도 중복 저장하지 않는다.
 */
@Service
@Transactional
public class SaveFinalTranscriptUseCase {

    private final MeetingTranscriptSegmentRepositoryPort repositoryPort;

    public SaveFinalTranscriptUseCase(MeetingTranscriptSegmentRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public SaveFinalTranscriptResult execute(SaveFinalTranscriptCommand command) {
        if (repositoryPort.existsBySourceEventId(command.sourceEventId())) {
            return new SaveFinalTranscriptResult(
                    command.meetingId(),
                    command.segmentId(),
                    command.sequence(),
                    false,
                    command.sourceEventId());
        }

        String normalizedText = requireText(command.text());
        TranscriptLanguage language = resolveLanguage(command.language());
        MeetingTranscriptSegment saved =
                repositoryPort.save(
                        MeetingTranscriptSegment.create(
                                command.meetingId(),
                                command.segmentId(),
                                command.sequence(),
                                language,
                                normalizedText,
                                // 현재 저장 기준은 원문 우선이므로, 번역문이 없으면 sourceText를 그대로 양쪽 컬럼에 넣는다.
                                normalizedText,
                                normalizedText,
                                command.startedAtMs(),
                                command.endedAtMs(),
                                command.sourceEventId()));

        return new SaveFinalTranscriptResult(
                saved.meetingId(),
                saved.segmentId(),
                saved.sequence(),
                true,
                saved.sourceEventId());
    }

    private String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "최종 원문 텍스트는 필수입니다.");
        }
        return text.trim();
    }

    private TranscriptLanguage resolveLanguage(String language) {
        if (language == null) {
            return TranscriptLanguage.UNKNOWN;
        }
        return switch (language.trim().toLowerCase()) {
            case "ko" -> TranscriptLanguage.KO;
            case "en" -> TranscriptLanguage.EN;
            default -> TranscriptLanguage.UNKNOWN;
        };
    }
}
