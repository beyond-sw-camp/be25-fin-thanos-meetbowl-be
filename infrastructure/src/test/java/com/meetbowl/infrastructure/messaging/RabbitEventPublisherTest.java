package com.meetbowl.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.meetbowl.common.event.EventTypes;
import com.meetbowl.infrastructure.messaging.meeting.MeetingEndedMessage;

/** 공통 RabbitEventPublisher가 모든 도메인 이벤트에 동일한 Envelope와 발행 설정을 적용하는지 검증한다. */
class RabbitEventPublisherTest {

    @Test
    void publishCommonEnvelope() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        confirmPublishedMessages(rabbitTemplate);
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
                        new RabbitMessagingProperties("meetbowl.topic", "api-server", 1, 1_000),
                        Clock.fixed(occurredAt, ZoneOffset.UTC));

        publisher.publish(EventTypes.DOCUMENT_INDEX_REQUESTED, new TestPayload("payload-value"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate)
                .send(
                        eq("meetbowl.topic"),
                        eq("document.index.requested"),
                        messageCaptor.capture(),
                        org.mockito.ArgumentMatchers.any(CorrelationData.class));

        Message message = messageCaptor.getValue();
        JsonNode envelope = objectMapper.readTree(message.getBody());

        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(message.getMessageProperties().getMessageId())
                .isEqualTo(envelope.get("eventId").asText());
        assertThat(message.getMessageProperties().getCorrelationId())
                .isEqualTo(envelope.get("correlationId").asText());
        assertThat(envelope.get("eventType").asText()).isEqualTo("document.index.requested");
        assertThat(envelope.get("producer").asText()).isEqualTo("api-server");
        assertThat(envelope.get("version").asInt()).isEqualTo(1);
        assertThat(envelope.get("occurredAt").asText()).isEqualTo(occurredAt.toString());
        assertThat(envelope.get("eventId").asText())
                .isEqualTo(envelope.get("correlationId").asText());
        assertThat(envelope.get("payload").get("value").asText()).isEqualTo("payload-value");
    }

    @Test
    void preserveProvidedCorrelationId() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        confirmPublishedMessages(rabbitTemplate);
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .findAndAddModules()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build();
        UUID correlationId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2099-01-01T01:00:00Z");
        Instant endedAt = Instant.parse("2099-01-01T02:00:00Z");
        RabbitEventPublisher publisher =
                new RabbitEventPublisher(
                        rabbitTemplate,
                        objectMapper,
                        new RabbitMessagingProperties("meetbowl.topic", "api-server", 1, 1_000),
                        Clock.fixed(Instant.parse("2099-01-01T02:00:00Z"), ZoneOffset.UTC));

        publisher.publish(
                EventTypes.MEETING_ENDED,
                new MeetingEndedMessage(
                        meetingId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "주간 전략 회의",
                        startedAt,
                        endedAt),
                correlationId);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate)
                .send(
                        eq("meetbowl.topic"),
                        eq("meeting.ended"),
                        messageCaptor.capture(),
                        org.mockito.ArgumentMatchers.any(CorrelationData.class));

        Message message = messageCaptor.getValue();
        JsonNode envelope = objectMapper.readTree(message.getBody());

        assertThat(envelope.get("correlationId").asText()).isEqualTo(correlationId.toString());
        assertThat(message.getMessageProperties().getCorrelationId())
                .isEqualTo(correlationId.toString());
        assertThat(envelope.get("eventId").asText()).isNotEqualTo(correlationId.toString());
        assertThat(envelope.get("payload").get("meetingId").asText())
                .isEqualTo(meetingId.toString());
        assertThat(envelope.get("payload").get("startedAt").asText())
                .isEqualTo(startedAt.toString());
        assertThat(envelope.get("payload").get("endedAt").asText()).isEqualTo(endedAt.toString());
    }

    @Test
    void brokerNack은발행실패로처리한다() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        completeConfirmation(rabbitTemplate, false, "broker rejected", null);
        RabbitEventPublisher publisher =
                publisher(rabbitTemplate, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThatThrownBy(
                        () ->
                                publisher.publish(
                                        EventTypes.MEETING_ENDED, new TestPayload("payload-value")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NACK");
    }

    @Test
    void 라우팅되지않은메시지는발행실패로처리한다() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ReturnedMessage returned =
                new ReturnedMessage(
                        MessageBuilder.withBody(new byte[0]).build(),
                        312,
                        "NO_ROUTE",
                        "meetbowl.topic",
                        "meeting.ended");
        completeConfirmation(rabbitTemplate, true, null, returned);
        RabbitEventPublisher publisher =
                publisher(rabbitTemplate, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThatThrownBy(
                        () ->
                                publisher.publish(
                                        EventTypes.MEETING_ENDED, new TestPayload("payload-value")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("라우팅되지 않았습니다");
    }

    private void confirmPublishedMessages(RabbitTemplate rabbitTemplate) {
        completeConfirmation(rabbitTemplate, true, null, null);
    }

    private void completeConfirmation(
            RabbitTemplate rabbitTemplate,
            boolean ack,
            String reason,
            ReturnedMessage returnedMessage) {
        doAnswer(
                        invocation -> {
                            CorrelationData correlationData = invocation.getArgument(3);
                            if (returnedMessage != null) {
                                correlationData.setReturned(returnedMessage);
                            }
                            correlationData
                                    .getFuture()
                                    .complete(new CorrelationData.Confirm(ack, reason));
                            return null;
                        })
                .when(rabbitTemplate)
                .send(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Message.class),
                        org.mockito.ArgumentMatchers.any(CorrelationData.class));
    }

    private RabbitEventPublisher publisher(RabbitTemplate rabbitTemplate, Clock clock) {
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .findAndAddModules()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build();
        return new RabbitEventPublisher(
                rabbitTemplate,
                objectMapper,
                new RabbitMessagingProperties("meetbowl.topic", "api-server", 1, 1_000),
                clock);
    }

    private record TestPayload(String value) {}
}
