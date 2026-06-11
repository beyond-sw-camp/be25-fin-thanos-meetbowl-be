package com.meetbowl.application.chatbot;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.chatbot.ChatAnswer;
import com.meetbowl.domain.chatbot.ChatConversationContext;
import com.meetbowl.domain.chatbot.ChatMessage;
import com.meetbowl.domain.chatbot.ChatMessageRole;
import com.meetbowl.domain.chatbot.ChatRequestContext;
import com.meetbowl.domain.chatbot.ChatSharedWorkspaceAccessPort;
import com.meetbowl.domain.chatbot.ChatbotAiPort;

/**
 * 챗봇 질의를 처리하는 Gateway UseCase다.
 *
 * <p>핵심 책임은 두 가지다. 첫째, 질문마다 현재 접근 가능한 공유 워크스페이스 범위를 다시 계산해 권한 상실이 즉시 반영되게 한다. 둘째, 검증된 요청 문맥만 AI 서버에
 * 위임하고 결과를 그대로 반환한다.
 *
 * <p>챗봇 대화는 업무 데이터가 아니므로 트랜잭션을 열지 않고 어떤 저장소에도 기록하지 않는다. 외부 AI 호출을 DB 트랜잭션 안에 묶지 않는 규칙과도 일치한다. 장애
 * 추적을 위한 로그에도 질문, 답변, 출처 본문은 남기지 않고 비민감 메타데이터만 남긴다.
 */
@Service
public class AskChatbotUseCase {

    private final ChatbotAiPort chatbotAiPort;
    private final ChatSharedWorkspaceAccessPort chatSharedWorkspaceAccessPort;

    public AskChatbotUseCase(
            ChatbotAiPort chatbotAiPort,
            ChatSharedWorkspaceAccessPort chatSharedWorkspaceAccessPort) {
        this.chatbotAiPort = chatbotAiPort;
        this.chatSharedWorkspaceAccessPort = chatSharedWorkspaceAccessPort;
    }

    public ChatAnswerResult execute(AskChatbotCommand command) {
        // 세션에 저장된 과거 권한을 쓰지 않고 매 요청 멤버십을 다시 계산한다.
        Set<UUID> sharedWorkspaceIds =
                chatSharedWorkspaceAccessPort.findAccessibleSharedWorkspaceIds(
                        command.userId(), command.organizationId());

        // 신뢰할 수 없는 외부 role 문자열을 도메인 역할로 좁힌 뒤, 도메인 경계에서 길이/교차 규칙을 검증한다.
        List<ChatMessage> messages =
                command.messageHistory() == null
                        ? List.of()
                        : command.messageHistory().stream().map(this::toDomainMessage).toList();
        ChatConversationContext conversation = new ChatConversationContext(messages);
        ChatRequestContext requestContext =
                new ChatRequestContext(
                        command.question(),
                        conversation,
                        command.userId(),
                        command.organizationId(),
                        sharedWorkspaceIds);

        ChatAnswer answer = chatbotAiPort.ask(requestContext);
        return ChatAnswerResult.from(answer);
    }

    private ChatMessage toDomainMessage(AskChatbotCommand.Message message) {
        return new ChatMessage(toRole(message.role()), message.content());
    }

    private ChatMessageRole toRole(String role) {
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 메시지 역할은 필수입니다.");
        }
        return switch (role.trim().toLowerCase()) {
            case "user" -> ChatMessageRole.USER;
            case "assistant" -> ChatMessageRole.ASSISTANT;
            default ->
                    throw new BusinessException(
                            ErrorCode.COMMON_INVALID_REQUEST,
                            "챗봇 메시지 역할은 user 또는 assistant만 허용합니다.");
        };
    }
}
