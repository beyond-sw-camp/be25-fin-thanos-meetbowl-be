package com.meetbowl.api.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.application.user.UserSearchReindexUseCase;
import com.meetbowl.common.event.EventEnvelope;
import com.meetbowl.common.event.EventTypes;
import com.meetbowl.common.event.user.UserSearchReindexRequestedMessage;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class UserSearchReindexRequestedListenerTest {

    @Test
    void consumeDelegatesScopedReindexRequest() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        UserSearchReindexUseCase useCase = org.mockito.Mockito.mock(UserSearchReindexUseCase.class);
        UserSearchReindexRequestedListener listener =
                new UserSearchReindexRequestedListener(objectMapper, useCase);
        UUID departmentId = UUID.randomUUID();
        UUID requestedByUserId = UUID.randomUUID();

        EventEnvelope<UserSearchReindexRequestedMessage> envelope =
                new EventEnvelope<>(
                        UUID.randomUUID(),
                        EventTypes.USER_SEARCH_REINDEX_REQUESTED,
                        Instant.parse("2026-06-18T00:00:00Z"),
                        "api-server",
                        1,
                        UUID.randomUUID(),
                        new UserSearchReindexRequestedMessage(
                                "DEPARTMENT_UPDATED",
                                false,
                                List.of(),
                                null,
                                departmentId,
                                null,
                                null,
                                requestedByUserId));

        listener.consume(
                objectMapper.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<UserSearchReindexUseCase.Command> commandCaptor =
                ArgumentCaptor.forClass(UserSearchReindexUseCase.Command.class);
        verify(useCase).execute(commandCaptor.capture());
        assertEquals(departmentId, commandCaptor.getValue().departmentId());
        assertEquals(false, commandCaptor.getValue().reindexAll());
    }
}
