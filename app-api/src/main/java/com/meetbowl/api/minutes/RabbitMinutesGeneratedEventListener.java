package com.meetbowl.api.minutes;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.meetbowl.application.minutes.SyncGeneratedMinutesCommand;
import com.meetbowl.application.minutes.SyncGeneratedMinutesUseCase;
import com.meetbowl.common.event.EventEnvelope;
import com.meetbowl.common.event.EventTypes;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/** AI 서버가 발행한 minutes.generated 이벤트를 저장용 application use case에 전달한다. */
@Component
public class RabbitMinutesGeneratedEventListener {

    private final ObjectMapper objectMapper;
    private final SyncGeneratedMinutesUseCase syncGeneratedMinutesUseCase;

    public RabbitMinutesGeneratedEventListener(
            ObjectMapper objectMapper, SyncGeneratedMinutesUseCase syncGeneratedMinutesUseCase) {
        this.objectMapper = objectMapper;
        this.syncGeneratedMinutesUseCase = syncGeneratedMinutesUseCase;
    }

    @RabbitListener(queues = "${meetbowl.rabbitmq.minutes-generated-queue:api.minutes.generated}")
    public void consume(byte[] body) {
        EventEnvelope<MinutesGeneratedMessage> envelope = readEnvelope(body);
        if (!EventTypes.MINUTES_GENERATED.equals(envelope.eventType())) {
            throw new IllegalArgumentException("지원하지 않는 이벤트입니다: " + envelope.eventType());
        }
        MinutesGeneratedMessage payload = envelope.payload();
        // 메시지 DTO를 바로 저장 계층에 넘기지 않고 application 명령으로 변환해 계층 경계를 유지한다.
        syncGeneratedMinutesUseCase.execute(
                new SyncGeneratedMinutesCommand(
                        payload.meetingId(),
                        payload.organizationId(),
                        payload.reviewerUserId(),
                        payload.summary(),
                        serializeEditorContent(payload),
                        payload.model(),
                        payload.promptVersion()));
    }

    private EventEnvelope<MinutesGeneratedMessage> readEnvelope(byte[] body) {
        try {
            JavaType envelopeType =
                    objectMapper
                            .getTypeFactory()
                            .constructParametricType(
                                    EventEnvelope.class, MinutesGeneratedMessage.class);
            return objectMapper.readValue(body, envelopeType);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("minutes.generated 이벤트를 읽을 수 없습니다.", exception);
        }
    }

    private String serializeEditorContent(MinutesGeneratedMessage payload) {
        try {
            return objectMapper.writeValueAsString(payload.editorContent());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "minutes.generated editorContent 직렬화에 실패했습니다.", exception);
        }
    }
}
