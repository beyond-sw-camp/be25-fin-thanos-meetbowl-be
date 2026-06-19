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
        /**
         * `meeting.ended`는 "회의가 DB에서 종료로 확정됐다"는 사실을 알리는 후속 처리 시작 이벤트다. 참석자 브라우저가 창을 닫았다는 사실 자체를 바로
         * 담는 이벤트가 아니라, authoritative 종료 기준(호스트 종료 또는 시스템 종료 처리 완료) 이후에만 발행한다.
         */
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
                    // 후속 AI 작업은 유실되면 안 되므로 persistent message와 messageId를 함께 세팅한다.
                    MessageProperties properties = message.getMessageProperties();
                    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    properties.setMessageId(envelope.eventId().toString());
                    properties.setCorrelationId(correlationId.toString());
                    return message;
                });
    }
}
