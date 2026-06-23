package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatConversationContextTest {

    @Test
    void copiesMessagesBecauseRequestContextMustNotChangeDuringAiExecution() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.user("배포 회의를 찾아줘"));
        messages.add(ChatMessage.assistant("관련 회의록을 찾았습니다."));

        ChatConversationContext context = new ChatConversationContext(messages);
        messages.add(ChatMessage.user("일정만 알려줘"));

        assertEquals(2, context.messages().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> context.messages().add(ChatMessage.assistant("변경 시도")));
    }

    @Test
    void rejectsConsecutiveMessagesFromSameRole() {
        assertThrows(
                BusinessException.class,
                () ->
                        new ChatConversationContext(
                                List.of(ChatMessage.user("첫 질문"), ChatMessage.user("두 번째 질문"))));
    }

    @Test
    void rejectsHistoryThatDoesNotContainCompletedUserAssistantTurns() {
        assertThrows(
                BusinessException.class,
                () -> new ChatConversationContext(List.of(ChatMessage.assistant("선행 답변"))));
        assertThrows(
                BusinessException.class,
                () -> new ChatConversationContext(List.of(ChatMessage.user("미완료 질문"))));
    }

    @Test
    void rejectsHistoryAboveRequestLimit() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int index = 0; index <= ChatConversationContext.MAX_MESSAGE_COUNT; index++) {
            ChatMessage message =
                    index % 2 == 0
                            ? ChatMessage.user("질문 " + index)
                            : ChatMessage.assistant("답변 " + index);
            messages.add(message);
        }

        assertThrows(BusinessException.class, () -> new ChatConversationContext(messages));
    }
}
