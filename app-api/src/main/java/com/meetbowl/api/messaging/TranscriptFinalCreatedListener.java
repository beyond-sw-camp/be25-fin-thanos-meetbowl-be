package com.meetbowl.api.messaging;

import java.io.IOException;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.transcript.SaveFinalTranscriptCommand;
import com.meetbowl.application.transcript.SaveFinalTranscriptUseCase;

/**
 * STT 서버가 발행한 `transcript.final.created`를 소비해 최종 원문 segment를 저장한다.
 *
 * <p>메시지 DTO를 바로 도메인으로 흘리지 않고 여기서 command로 변환해 Application 계층 경계를 지킨다.
 */
@Component
public class TranscriptFinalCreatedListener {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SaveFinalTranscriptUseCase saveFinalTranscriptUseCase;

    public TranscriptFinalCreatedListener(SaveFinalTranscriptUseCase saveFinalTranscriptUseCase) {
        this.saveFinalTranscriptUseCase = saveFinalTranscriptUseCase;
    }

    @RabbitListener(queues = RabbitMqMessagingConfig.TRANSCRIPT_FINAL_SAVE_QUEUE)
    public void onTranscriptFinalCreated(String rawMessage) throws IOException {
        JsonNode root = objectMapper.readTree(rawMessage);
        JsonNode payload = root.path("payload");

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
                        payload.path("endedAtMs").isMissingNode() || payload.path("endedAtMs").isNull()
                                ? null
                                : payload.path("endedAtMs").asLong(),
                        payload.path("provider").asText(),
                        payload.path("idempotencyKey").asText()));
    }

    private UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
