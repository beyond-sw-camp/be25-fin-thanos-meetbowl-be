package com.meetbowl.infrastructure.client.chatbot.dto;

import java.util.List;
import java.util.UUID;

import com.meetbowl.domain.chatbot.ChatMessage;
import com.meetbowl.domain.chatbot.ChatRequestContext;

/**
 * meetbowl-ai 챗봇 API로 보내는 외부 클라이언트 DTO다.
 *
 * <p>검색 권한은 인증된 userId와 BE가 재계산한 sharedWorkspaceIds로만 표현한다. 모델은 권한 범위를 입력으로 받지 않으므로 이 요청에 권한 범위를
 * 임의로 넓힐 수 있는 필드를 추가하지 않는다.
 */
public record AiChatRequest(
        UUID requestId,
        UUID correlationId,
        UUID userId,
        String question,
        List<Message> messageHistory,
        List<UUID> sharedWorkspaceIds) {

    /** AI 서버 계약은 소문자 role을 사용하므로 도메인 enum을 소문자 문자열로 변환해 전달한다. */
    public record Message(String role, String content) {}

    public static AiChatRequest from(
            ChatRequestContext context, UUID requestId, UUID correlationId) {
        List<Message> messages =
                context.conversation().messages().stream().map(AiChatRequest::toMessage).toList();
        return new AiChatRequest(
                requestId,
                correlationId,
                context.userId(),
                context.question(),
                messages,
                List.copyOf(context.sharedWorkspaceIds()));
    }

    private static Message toMessage(ChatMessage message) {
        return new Message(message.role().name().toLowerCase(), message.content());
    }
}
