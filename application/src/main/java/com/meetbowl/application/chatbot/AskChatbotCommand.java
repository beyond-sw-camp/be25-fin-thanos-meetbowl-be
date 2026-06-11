package com.meetbowl.application.chatbot;

import java.util.List;
import java.util.UUID;

/**
 * 챗봇 질의 UseCase의 입력 모델이다.
 *
 * <p>사용자 ID와 조직 ID는 API Request DTO가 아니라 인증 결과에서 채워야 모델이 권한 범위를 임의로 지정할 수 없다. messageHistory는 현재
 * 화면이 전달한 휘발성 대화 문맥이며, role은 도메인 enum이 아닌 문자열로 받아 UseCase가 신뢰 가능한 역할로 변환한다.
 */
public record AskChatbotCommand(
        UUID userId, UUID organizationId, String question, List<Message> messageHistory) {

    /** 대화 이력 한 개 메시지다. role은 user/assistant 문자열로 전달되고 UseCase에서 도메인 역할로 좁혀진다. */
    public record Message(String role, String content) {}
}
