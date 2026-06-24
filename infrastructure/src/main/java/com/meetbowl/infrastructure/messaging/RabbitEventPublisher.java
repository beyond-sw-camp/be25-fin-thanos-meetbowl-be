package com.meetbowl.infrastructure.messaging;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.event.EventEnvelope;

/** 개별 도메인 Publisher가 공통 Envelope와 RabbitMQ 발행 세부 구현을 반복하지 않도록 제공하는 발행기다. */
@Component
public class RabbitEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitMessagingProperties properties;
    private final Clock clock;

    public RabbitEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            RabbitMessagingProperties properties,
            Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public void publish(String eventType, Object payload) {
        UUID eventId = UUID.randomUUID();

        // 요청 correlation 전파 체계가 아직 없으므로 이벤트 ID를 correlation ID로 사용한다.
        // 추후 요청 추적 Port가 도입되면 correlation ID만 외부에서 전달받도록 확장한다.
        publish(eventType, payload, eventId, eventId, Instant.now(clock));
    }

    public void publish(String eventType, Object payload, UUID correlationId) {
        publish(eventType, payload, UUID.randomUUID(), correlationId, Instant.now(clock));
    }

    public void publish(
            String eventType,
            Object payload,
            UUID eventId,
            UUID correlationId,
            Instant occurredAt) {
        EventEnvelope<Object> envelope =
                new EventEnvelope<>(
                        eventId,
                        eventType,
                        occurredAt,
                        properties.producer(),
                        properties.eventVersion(),
                        correlationId,
                        payload);

        CorrelationData correlationData = new CorrelationData(eventId.toString());
        rabbitTemplate.send(
                properties.exchange(),
                eventType,
                persistentJsonMessage(eventType, eventId, correlationId, envelope),
                correlationData);
        awaitBrokerConfirmation(eventType, eventId, correlationData);
    }

    private Message persistentJsonMessage(
            String eventType, UUID eventId, UUID correlationId, EventEnvelope<Object> envelope) {
        try {
            // Spring Boot 4의 전역 Jackson 3 설정을 사용해 API와 이벤트의 시간 직렬화 규칙을 통일한다.
            // occurredAt이 UTC ISO-8601 문자열인지 여부는 공통 Publisher 테스트에서 계약으로 검증한다.
            return MessageBuilder.withBody(objectMapper.writeValueAsBytes(envelope))
                    .setContentType("application/json")
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(eventId.toString())
                    .setCorrelationId(correlationId.toString())
                    .build();
        } catch (JacksonException exception) {
            // 직렬화 실패는 발행 성공으로 숨기지 않고 업무 요청까지 실패시켜 재처리 가능하게 한다.
            throw new IllegalStateException(eventType + " 이벤트 직렬화에 실패했습니다.", exception);
        }
    }

    private void awaitBrokerConfirmation(
            String eventType, UUID eventId, CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm =
                    correlationData
                            .getFuture()
                            .get(properties.confirmTimeoutMillis(), TimeUnit.MILLISECONDS);
            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                throw new IllegalStateException(
                        eventType
                                + " 이벤트가 어떤 Queue에도 라우팅되지 않았습니다. eventId="
                                + eventId
                                + ", replyText="
                                + returned.getReplyText());
            }
            if (!confirm.ack()) {
                throw new IllegalStateException(
                        eventType
                                + " 이벤트가 broker에서 NACK 처리됐습니다. eventId="
                                + eventId
                                + ", reason="
                                + confirm.reason());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    eventType + " 이벤트 confirm 대기 중 중단됐습니다. eventId=" + eventId, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    eventType + " 이벤트 broker confirm을 확인하지 못했습니다. eventId=" + eventId, exception);
        }
    }
}
