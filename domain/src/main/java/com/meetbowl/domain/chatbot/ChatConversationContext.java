package com.meetbowl.domain.chatbot;

import java.util.List;

/**
 * 후속 질문을 이해하는 데 필요한 최소 대화 문맥이다.
 *
 * <p>서버 저장 대신 클라이언트가 매 요청에 문맥을 전달하므로, 무제한 입력과 임의 역할 삽입을 이 경계에서 먼저 차단한다. 모델별 token 계산은 Provider마다 달라
 * 이 계층에서 정확히 판단할 수 없으므로 AI 연동 계층이 별도 token budget을 적용해야 한다.
 */
public record ChatConversationContext(List<ChatMessage> messages) {

    public static final int MAX_MESSAGE_COUNT = 20;
    public static final int MAX_TOTAL_CONTENT_LENGTH = 40_000;

    public ChatConversationContext {
        messages = messages == null ? List.of() : messages;
        if (messages.size() > MAX_MESSAGE_COUNT) {
            throw ChatDomainValidators.invalid("챗봇 대화 이력은 최대 20개 메시지까지 전달할 수 있습니다.");
        }
        if (messages.stream().anyMatch(message -> message == null)) {
            throw ChatDomainValidators.invalid("챗봇 대화 이력에 빈 메시지를 포함할 수 없습니다.");
        }
        messages = List.copyOf(messages);
        if (!messages.isEmpty() && messages.get(0).role() != ChatMessageRole.USER) {
            throw ChatDomainValidators.invalid("챗봇 대화 이력은 사용자 메시지로 시작해야 합니다.");
        }
        if (!messages.isEmpty()
                && messages.get(messages.size() - 1).role() != ChatMessageRole.ASSISTANT) {
            throw ChatDomainValidators.invalid("챗봇 대화 이력은 AI 답변까지 완료된 문맥이어야 합니다.");
        }
        int totalContentLength =
                messages.stream().mapToInt(message -> message.content().length()).sum();
        if (totalContentLength > MAX_TOTAL_CONTENT_LENGTH) {
            throw ChatDomainValidators.invalid("챗봇 대화 이력 전체 내용은 40000자 이하여야 합니다.");
        }

        for (int index = 1; index < messages.size(); index++) {
            if (messages.get(index - 1).role() == messages.get(index).role()) {
                throw ChatDomainValidators.invalid("챗봇 대화 이력의 사용자와 AI 메시지는 번갈아야 합니다.");
            }
        }
    }

    public static ChatConversationContext empty() {
        return new ChatConversationContext(List.of());
    }
}
