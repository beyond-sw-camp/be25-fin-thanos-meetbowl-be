package com.meetbowl.domain.chatbot;

/**
 * 한 번의 챗봇 실행에 필요한 사용자 입력과 신뢰된 권한 범위를 묶는다.
 *
 * <p>대화 세션 대신 요청 단위 객체를 사용해야 서버가 이전 대화를 보유하지 않아도 후속 질문을 처리할 수 있고, 실행이 끝난 뒤 폐기해야 할 데이터 경계도 분명해진다.
 */
public record ChatRequestContext(
        String question, ChatConversationContext conversation, ChatAccessContext accessContext) {

    public ChatRequestContext {
        question =
                ChatDomainValidators.requireText(
                        question,
                        ChatMessage.MAX_CONTENT_LENGTH,
                        "챗봇 질문은 필수입니다.",
                        "챗봇 질문은 20000자 이하여야 합니다.");
        conversation = conversation == null ? ChatConversationContext.empty() : conversation;
        if (accessContext == null) {
            throw ChatDomainValidators.invalid("챗봇 검색 권한 범위는 필수입니다.");
        }
    }
}
