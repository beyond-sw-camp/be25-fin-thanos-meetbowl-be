package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatAnswerTest {

    @Test
    void keepsValidatedModelMetadataWithoutPersistenceIdentity() {
        ChatAnswer answer = new ChatAnswer("확인된 일정입니다.", List.of(), "configured-model", "chat-v1");

        assertEquals("configured-model", answer.modelName());
        assertEquals("chat-v1", answer.promptVersion());
    }

    @Test
    void rejectsDuplicateCitationDisplayOrder() {
        ChatCitation first = citation(UUID.randomUUID(), 1);
        ChatCitation second = citation(UUID.randomUUID(), 1);

        assertThrows(
                BusinessException.class,
                () -> new ChatAnswer("답변", List.of(first, second), "configured-model", "chat-v1"));
    }

    private ChatCitation citation(UUID sourceId, int displayOrder) {
        return new ChatCitation(
                ChatSourceType.MINUTES, sourceId, "회의록", "근거", null, 0.9D, displayOrder);
    }
}
