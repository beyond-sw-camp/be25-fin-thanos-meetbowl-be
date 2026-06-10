package com.meetbowl.domain.chatbot;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 한 번의 챗봇 요청에 전달되는 휘발성 대화 메시지다.
 *
 * <p>식별자와 생성 시각을 의도적으로 두지 않는다. 영속 수명주기를 암시하는 필드가 생기면 대화를 저장하지 않는 정책과 API 계약이 다시 분리될 수 있기 때문이다.
 */
public record ChatMessage(ChatMessageRole role, String content) {

    public static final int MAX_CONTENT_LENGTH = 20_000;

    public ChatMessage {
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 메시지 역할은 필수입니다.");
        }
        content =
                ChatDomainValidators.requireText(
                        content,
                        MAX_CONTENT_LENGTH,
                        "챗봇 메시지 내용은 필수입니다.",
                        "챗봇 메시지 내용은 20000자 이하여야 합니다.");
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatMessageRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatMessageRole.ASSISTANT, content);
    }
}
