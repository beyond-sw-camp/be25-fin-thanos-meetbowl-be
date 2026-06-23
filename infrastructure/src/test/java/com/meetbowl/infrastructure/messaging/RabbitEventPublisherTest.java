package com.meetbowl.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.meetbowl.common.event.EventTypes;

/** 공통 RabbitEventPublisher가 모든 도메인 이벤트에 동일한 Envelope와 발행 설정을 적용하는지 검증한다. */
class RabbitEventPublisherTest {

    @Test
    void publishCommonEnvelope() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        // 운영에서는 Spring Boot 전역 Jackson 설정으로 Instant가 ISO-8601 문자열로 직렬화되므로 테스트도 같은 규칙을 맞춘다.
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .findAndAddModules()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build();
        Instant occurredAt = Instant.parse("2099-01-01T02:00:00Z");
        RabbitEventPublisher publisher =
                new RabbitEventPublisher(
                        rabbitTemplate,
                        objectMapper,
                        new RabbitMessagingProperties("meetbowl.topic", "api-server", 1),
                        Clock.fixed(occurredAt, ZoneOffset.UTC));

        publisher.publish(EventTypes.DOCUMENT_INDEX_REQUESTED, new TestPayload("payload-value"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate)
                .send(
                        eq("meetbowl.topic"),
                        eq("document.index.requested"),
                        messageCaptor.capture());

        Message message = messageCaptor.getValue();
        JsonNode envelope = objectMapper.readTree(message.getBody());

        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(envelope.get("eventType").asText()).isEqualTo("document.index.requested");
        assertThat(envelope.get("producer").asText()).isEqualTo("api-server");
        assertThat(envelope.get("version").asInt()).isEqualTo(1);
        assertThat(envelope.get("occurredAt").asText()).isEqualTo(occurredAt.toString());
        assertThat(envelope.get("eventId").asText())
                .isEqualTo(envelope.get("correlationId").asText());
        assertThat(envelope.get("payload").get("value").asText()).isEqualTo("payload-value");
    }

    private record TestPayload(String value) {}
}
