package com.meetbowl.application.transcript;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;
import com.meetbowl.domain.transcript.TranscriptLanguage;

/**
 * STT 서버로부터 수신된 최종 발화 세그먼트(Final Transcript)를 데이터베이스에 멱등하게 저장하는 유스케이스입니다.
 *
 * <p>이 유스케이스는 "정말 DB에 남기는 단계"다.
 * STREAMING 자막은 이 경로에 들어오지 않고, STT가 FINALIZED로 확정한 세그먼트만 들어온다.
 *
 * <p>앞단 흐름:
 * 1. SegmentController.finalize()
 * 2. RabbitMqTranscriptPublisher.publishFinalSegment()
 * 3. TranscriptFinalCreatedListener.onTranscriptFinalCreated()
 * 4. SaveFinalTranscriptUseCase.execute()  ← 현재 파일
 *
 * <p>중복 수신된 이벤트는 무시하며, 텍스트 정규화 및 언어 코드 매핑을 수행합니다.
 */
@Service
@Transactional
public class SaveFinalTranscriptUseCase {

    private final MeetingTranscriptSegmentRepositoryPort repositoryPort;

    public SaveFinalTranscriptUseCase(MeetingTranscriptSegmentRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    /**
     * [최종 원문 저장 실행]
     * 1. 멱등성 검사: 동일한 sourceEventId를 가진 데이터가 이미 존재하는지 확인합니다.
     * 2. 데이터 가공: 텍스트 공백 제거 및 언어 코드를 도메인 타입으로 변환합니다.
     * 3. 영속화: MeetingTranscriptSegment 도메인 모델을 생성하여 저장소에 기록합니다.
     *
     * @param command 저장할 자막 세그먼트 정보 및 메타데이터
     * @return 저장 결과 (성공 여부 및 저장된 정보 요약)
     */
    public SaveFinalTranscriptResult execute(SaveFinalTranscriptCommand command) {
        /**
         * STT -> RabbitMQ -> BE 경로는 네트워크 재시도, 브로커 재전달, consumer 재기동 때문에
         * 같은 finalized segment가 다시 들어올 수 있다.
         *
         * 그래서 이 단계의 핵심 규칙은:
         * - "같은 문장처럼 보이는가?"가 아니라
         * - "같은 sourceEventId(eventId)를 가진 메시지를 이미 처리했는가?"다.
         *
         * 즉, 화면에서 비슷한 문장이 보였다는 사실과 DB 멱등성 판단 기준은 다르다.
         * DB 저장 기준은 sourceEventId다.
         */
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
        
        /**
         * 저장 기준은 항상 finalized segment다.
         *
         * 이 말의 정확한 뜻:
         * - 사용자가 말하는 중에 흔들리던 중간 delta는 저장하지 않는다.
         * - STT가 최종적으로 "이 문장은 여기까지가 확정"이라고 판단한 한 문장 단위만 저장한다.
         * - 그래서 DB에서 회의 원문을 조회하면 STREAMING 흔적 없이 확정 문장 목록만 나온다.
         */
        MeetingTranscriptSegment saved =
                repositoryPort.save(
                        MeetingTranscriptSegment.create(
                                command.meetingId(),
                                command.segmentId(),
                                command.sequence(),
                                language,
                                normalizedText,
                                // 현재 정책상 ko/en 표시 텍스트를 원문으로 동일하게 채운다.
                                // 추후 STT가 번역본을 함께 보내기 시작하면 이 자리에서 분리 저장하게 된다.
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

    /** 입력 텍스트의 유효성을 검증하고 전후 공백을 제거합니다. */
    private String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "최종 원문 텍스트는 필수입니다.");
        }
        return text.trim();
    }

    /** 문자열 언어 코드를 도메인 전용 Enum 타입으로 안전하게 변환합니다. */
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
