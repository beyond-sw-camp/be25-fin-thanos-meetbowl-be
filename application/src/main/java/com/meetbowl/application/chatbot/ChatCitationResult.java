package com.meetbowl.application.chatbot;

import java.util.UUID;

import com.meetbowl.domain.chatbot.ChatCitation;

/**
 * 챗봇 답변 출처의 application 출력 모델이다.
 *
 * <p>app-api가 domain 타입에 의존하지 않도록 sourceType은 도메인 enum 대신 문자열 값으로 노출한다. 변환은 이 경계에서 한 번만 수행한다.
 */
public record ChatCitationResult(
        String sourceType,
        UUID sourceId,
        String title,
        String snippet,
        String sourceUri,
        Double score,
        int displayOrder) {

    public static ChatCitationResult from(ChatCitation citation) {
        return new ChatCitationResult(
                citation.sourceType().name(),
                citation.sourceId(),
                citation.title(),
                citation.snippet(),
                citation.sourceUri(),
                citation.score(),
                citation.displayOrder());
    }
}
