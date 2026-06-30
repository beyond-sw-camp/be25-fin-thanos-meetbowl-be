package com.meetbowl.api.messaging;

import java.io.IOException;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.meetbowl.api.config.ConditionalOnMeetbowlAppRole;
import com.meetbowl.api.config.MeetbowlAppRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.transcript.SaveFinalTranscriptCommand;
import com.meetbowl.application.transcript.SaveFinalTranscriptUseCase;

/**
 * STT 서버가 발행한 `transcript.final.created`를 소비해 최종 원문 segment를 저장한다.
 *
 * <p>이 Listener는 "RabbitMQ 메시지 세계"와 "BE 애플리케이션 유스케이스 세계"의 경계다.
 *
 * <p>흐름:
 * 1. meetbowl-stt의 SegmentController가 어떤 문장을 FINALIZED로 확정한다.
 * 2. STT의 RabbitMqTranscriptPublisher가 `transcript.final.created`를 발행한다.
 * 3. 이 Listener가 `api.transcript.final.save` 큐에서 그 메시지를 받는다.
 * 4. 외부 메시지 형태를 SaveFinalTranscriptCommand로 변환한다.
 * 5. SaveFinalTranscriptUseCase가 멱등성 검사 후 DB에 저장한다.
 *
 * <p>메시지 DTO를 바로 도메인으로 흘리지 않고 여기서 command로 변환해 Application 계층 경계를 지킨다.
 */
@Component
@ConditionalOnMeetbowlAppRole({MeetbowlAppRole.ALL, MeetbowlAppRole.WORKER})
public class TranscriptFinalCreatedListener {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SaveFinalTranscriptUseCase saveFinalTranscriptUseCase;

    public TranscriptFinalCreatedListener(SaveFinalTranscriptUseCase saveFinalTranscriptUseCase) {
        this.saveFinalTranscriptUseCase = saveFinalTranscriptUseCase;
    }

    @RabbitListener(queues = RabbitMqMessagingConfig.TRANSCRIPT_FINAL_SAVE_QUEUE)
    public void onTranscriptFinalCreated(String rawMessage) throws IOException {
        /**
         * STT 서버가 보내는 메시지는 공통 EventEnvelope 구조를 따르므로,
         * 여기서는 envelope 메타데이터(eventId, correlationId)와 payload 본문을 분리해서 읽는다.
         *
         * 중요한 점:
         * - eventId는 DB 저장 멱등성 키(sourceEventId)로 그대로 내려간다.
         * - correlationId는 FE -> BE -> STT -> RabbitMQ까지 이어진 요청 흐름 추적용이다.
         * - payload의 text/language/sequence는 "finalized segment" 기준 데이터만 들어온다.
         *
         * 즉, 이 Listener는 STREAMING caption을 저장하지 않는다.
         * 이미 STT가 "이 문장은 최종 확정해도 된다"고 판단한 건만 받는다.
         */
        JsonNode root = objectMapper.readTree(rawMessage);
        JsonNode payload = root.path("payload");

        /**
         * Listener는 Message DTO를 그대로 저장소로 넘기지 않는다.
         * RabbitMQ 메시지는 외부 계약이고, SaveFinalTranscriptUseCase는 애플리케이션 내부 계약이므로
         * 이 경계에서 명시적으로 Command 객체로 바꿔 계층 규칙을 지킨다.
         *
         * 이 변환 단계가 필요한 이유:
         * - 외부 이벤트 payload 구조가 바뀌어도 Application 계층 영향 범위를 제한하려고
         * - Jackson JsonNode를 UseCase까지 들고 올라가지 않게 하려고
         * - sourceEventId(eventId), segmentId, meetingId 등 핵심 식별자를 명시적으로 검증하려고
         */
        saveFinalTranscriptUseCase.execute(
                new SaveFinalTranscriptCommand(
                        UUID.fromString(root.path("eventId").asText()),
                        parseUuid(root.path("correlationId").asText(null)),
                        UUID.fromString(payload.path("meetingId").asText()),
                        UUID.fromString(payload.path("sessionId").asText()),
                        payload.path("segmentId").asText(),
                        payload.path("sequence").asLong(),
                        payload.path("language").asText(),
                        payload.path("text").asText(),
                        payload.path("startedAtMs").isMissingNode()
                                ? null
                                : payload.path("startedAtMs").asLong(),
                        payload.path("endedAtMs").isMissingNode()
                                        || payload.path("endedAtMs").isNull()
                                ? null
                                : payload.path("endedAtMs").asLong(),
                        payload.path("provider").asText(),
                        payload.path("idempotencyKey").asText()));
    }

    /** correlationId는 선택값이므로 비어 있으면 null로 내려 후속 계층에서 새 값을 만들 수 있게 둔다. */
    private UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
