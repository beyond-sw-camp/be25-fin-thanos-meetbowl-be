package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatMessageTest {

    @Test
    void normalizesMessageContent() {
        ChatMessage message = ChatMessage.user("  지난 회의 결정사항 알려줘  ");

        assertEquals(ChatMessageRole.USER, message.role());
        assertEquals("지난 회의 결정사항 알려줘", message.content());
    }

    @Test
    void rejectsBlankMessage() {
        assertThrows(BusinessException.class, () -> ChatMessage.assistant("  "));
    }
}
