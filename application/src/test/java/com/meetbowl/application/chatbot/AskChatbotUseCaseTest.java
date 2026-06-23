package com.meetbowl.application.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.domain.chatbot.ChatAnswer;
import com.meetbowl.domain.chatbot.ChatCitation;
import com.meetbowl.domain.chatbot.ChatRequestContext;
import com.meetbowl.domain.chatbot.ChatSharedWorkspaceAccessPort;
import com.meetbowl.domain.chatbot.ChatSourceType;
import com.meetbowl.domain.chatbot.ChatbotAiPort;

@ExtendWith(MockitoExtension.class)
class AskChatbotUseCaseTest {

    @Mock private ChatbotAiPort chatbotAiPort;
    @Mock private ChatSharedWorkspaceAccessPort chatSharedWorkspaceAccessPort;

    @Test
    @DisplayName("질문마다 공유 워크스페이스 권한을 다시 계산해 AI 요청 문맥에 반영한다")
    void execute_recomputesSharedWorkspaceAccess() {
        AskChatbotUseCase useCase =
                new AskChatbotUseCase(chatbotAiPort, chatSharedWorkspaceAccessPort);

        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        given(
                        chatSharedWorkspaceAccessPort.findAccessibleSharedWorkspaceIds(
                                userId, organizationId))
                .willReturn(Set.of(workspaceId));
        given(chatbotAiPort.ask(any())).willReturn(sampleAnswer());

        AskChatbotCommand command =
                new AskChatbotCommand(
                        userId,
                        organizationId,
                        "지난 회의 배포 일정 알려줘",
                        List.of(
                                new AskChatbotCommand.Message("user", "배포 관련 회의를 찾아줘"),
                                new AskChatbotCommand.Message("assistant", "관련 회의록을 찾았습니다.")));

        ChatAnswerResult result = useCase.execute(command);

        // 권한 재계산은 요청마다 일어나야 한다.
        verify(chatSharedWorkspaceAccessPort)
                .findAccessibleSharedWorkspaceIds(userId, organizationId);

        ArgumentCaptor<ChatRequestContext> contextCaptor =
                ArgumentCaptor.forClass(ChatRequestContext.class);
        verify(chatbotAiPort).ask(contextCaptor.capture());
        ChatRequestContext sentContext = contextCaptor.getValue();
        assertEquals(userId, sentContext.userId());
        assertEquals(Set.of(workspaceId), sentContext.sharedWorkspaceIds());
        assertEquals(2, sentContext.conversation().messages().size());

        assertEquals("6월 10일까지 1차 배포", result.answer());
        assertEquals(1, result.citations().size());
        assertEquals("gemini-2.0-flash", result.modelName());
    }

    @Test
    @DisplayName("접근 가능한 공유 워크스페이스가 없으면 빈 범위로 위임한다")
    void execute_withNoSharedWorkspaceAccess() {
        AskChatbotUseCase useCase =
                new AskChatbotUseCase(chatbotAiPort, chatSharedWorkspaceAccessPort);

        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        given(
                        chatSharedWorkspaceAccessPort.findAccessibleSharedWorkspaceIds(
                                userId, organizationId))
                .willReturn(Set.of());
        given(chatbotAiPort.ask(any())).willReturn(sampleAnswer());

        useCase.execute(new AskChatbotCommand(userId, organizationId, "질문", List.of()));

        ArgumentCaptor<ChatRequestContext> contextCaptor =
                ArgumentCaptor.forClass(ChatRequestContext.class);
        verify(chatbotAiPort).ask(contextCaptor.capture());
        assertTrue(contextCaptor.getValue().sharedWorkspaceIds().isEmpty());
    }

    private ChatAnswer sampleAnswer() {
        ChatCitation citation =
                new ChatCitation(
                        ChatSourceType.MINUTES,
                        UUID.randomUUID(),
                        "배포 일정 회의록",
                        "6월 10일까지 1차 배포",
                        null,
                        0.9D,
                        1);
        return new ChatAnswer("6월 10일까지 1차 배포", List.of(citation), "gemini-2.0-flash", "chat-v1");
    }
}
