package com.meetbowl.infrastructure.messaging.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.application.minutes.SyncGeneratedMinutesCommand;
import com.meetbowl.application.minutes.SyncGeneratedMinutesUseCase;
import com.meetbowl.common.event.EventEnvelope;
import com.meetbowl.common.event.EventTypes;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RabbitMinutesGeneratedEventListenerTest {

    @Test
    void consumeStoresGeneratedMinutesDraft() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        SyncGeneratedMinutesUseCase useCase = org.mockito.Mockito.mock(SyncGeneratedMinutesUseCase.class);
        RabbitMinutesGeneratedEventListener listener =
                new RabbitMinutesGeneratedEventListener(objectMapper, useCase);

        EventEnvelope<MinutesGeneratedMessage> envelope =
                new EventEnvelope<>(
                        UUID.randomUUID(),
                        EventTypes.MINUTES_GENERATED,
                        java.time.Instant.parse("2026-06-12T00:00:00Z"),
                        "ai-server",
                        1,
                        UUID.randomUUID(),
                        new MinutesGeneratedMessage(
                                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                                "DRAFT",
                                "회의 요약",
                                List.of(),
                                List.of("결정사항 1"),
                                List.of(),
                                objectMapper.readTree(
                                        "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"회의록 본문\"}]}]}"),
                                "llm-model-name",
                                "minutes-v1"));

        listener.consume(objectMapper.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<SyncGeneratedMinutesCommand> captor =
                ArgumentCaptor.forClass(SyncGeneratedMinutesCommand.class);
        verify(useCase).execute(captor.capture());
        assertEquals("회의 요약", captor.getValue().summary());
        assertEquals(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"회의록 본문\"}]}]}",
                captor.getValue().content());
    }
}
