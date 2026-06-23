package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatRequestContextTest {

    @Test
    void createsStatelessRequestWithEmptyConversation() {
        ChatRequestContext request =
                new ChatRequestContext(
                        "  배포 일정을 알려줘  ", null, UUID.randomUUID(), UUID.randomUUID(), Set.of());

        assertEquals("배포 일정을 알려줘", request.question());
        assertEquals(ChatConversationContext.empty(), request.conversation());
    }

    @Test
    void requiresUserId() {
        assertThrows(
                BusinessException.class,
                () ->
                        new ChatRequestContext(
                                "질문",
                                ChatConversationContext.empty(),
                                null,
                                UUID.randomUUID(),
                                Set.of()));
    }

    @Test
    void requiresOrganizationId() {
        assertThrows(
                BusinessException.class,
                () ->
                        new ChatRequestContext(
                                "질문",
                                ChatConversationContext.empty(),
                                UUID.randomUUID(),
                                null,
                                Set.of()));
    }

    @Test
    void copiesSharedWorkspaceIdsSoCallerCannotWidenScopeDuringAiExecution() {
        Set<UUID> workspaceIds = new HashSet<>();
        workspaceIds.add(UUID.randomUUID());

        ChatRequestContext request =
                new ChatRequestContext(
                        "질문", null, UUID.randomUUID(), UUID.randomUUID(), workspaceIds);
        workspaceIds.add(UUID.randomUUID());

        assertEquals(1, request.sharedWorkspaceIds().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> request.sharedWorkspaceIds().add(UUID.randomUUID()));
    }
}
