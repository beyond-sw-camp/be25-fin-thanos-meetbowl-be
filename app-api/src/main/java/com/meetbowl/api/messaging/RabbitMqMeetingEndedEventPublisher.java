package com.meetbowl.api.messaging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import com.meetbowl.application.meeting.MeetingEndedEventPublisher;
import com.meetbowl.common.event.EventEnvelope;

/**
 * 회의 종료 후처리 시작 이벤트를 RabbitMQ로 발행한다.
 *
 * <p>회의 종료 자체는 동기 트랜잭션으로 끝내고, AI 회의록 생성 같은 긴 작업은 이 publisher가 `meeting.ended`로 넘긴다.
 */
@Component
public class RabbitMqMeetingEndedEventPublisher implements MeetingEndedEventPublisher {

    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    public RabbitMqMeetingEndedEventPublisher(
            org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishMeetingEnded(
            UUID meetingId,
            UUID hostUserId,
            UUID reviewerUserId,
            String title,
            Instant startedAt,
            Instant endedAt,
            UUID correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("meetingId", meetingId);
        payload.put("organizationId", null);
        payload.put("hostUserId", hostUserId);
        payload.put("reviewerUserId", reviewerUserId);
        payload.put("title", title);
        payload.put("startedAt", startedAt);
        payload.put("endedAt", endedAt);

        EventEnvelope<Map<String, Object>> envelope =
                new EventEnvelope<>(
                        UUID.randomUUID(),
                        "meeting.ended",
                        Instant.now(),
                        "api-server",
                        1,
                        correlationId,
                        payload);

        rabbitTemplate.convertAndSend(
                RabbitMqMessagingConfig.TOPIC_EXCHANGE,
                "meeting.ended",
                envelope,
                message -> {
                    MessageProperties properties = message.getMessageProperties();
                    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    properties.setMessageId(envelope.eventId().toString());
                    properties.setCorrelationId(correlationId.toString());
                    return message;
                });
    }
}
