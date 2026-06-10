package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatMessageTest {

    @Test
    void createUserAndAssistantMessages() {
        UUID sessionId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");

        ChatMessage userMessage =
                ChatMessage.user(sessionId, senderUserId, 1, "지난 회의 결정사항 알려줘", now);
        ChatMessage assistantMessage =
                ChatMessage.assistant(
                        sessionId, 2, "결정사항은 다음과 같습니다.", "gemini-2.5", "chat-v1", "ai-req-1", now);

        assertTrue(userMessage.isFromUser());
        assertTrue(assistantMessage.isAssistantAnswer());
        assertNull(assistantMessage.senderUserId());
        assertEquals("chat-v1", assistantMessage.promptVersion());
    }

    @Test
    void assistantMessageCannotHaveSenderUserId() {
        UUID sessionId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");

        assertThrows(
                BusinessException.class,
                () ->
                        ChatMessage.of(
                                null,
                                sessionId,
                                ChatMessageRole.ASSISTANT,
                                senderUserId,
                                1,
                                "답변",
                                null,
                                null,
                                null,
                                now));
    }
}
