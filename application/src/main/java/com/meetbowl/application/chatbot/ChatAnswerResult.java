package com.meetbowl.application.chatbot;

import java.util.List;

import com.meetbowl.domain.chatbot.ChatAnswer;

/**
 * 챗봇 질의 UseCase의 출력 모델이다.
 *
 * <p>스키마 검증을 마친 도메인 결과를 그대로 상위로 노출하지 않고 application 계약으로 한 번 더 분리한다. 이 값은 응답 후 저장하지 않으며 app-api에서
 * API Response DTO로 변환된다.
 */
public record ChatAnswerResult(
        String answer, List<ChatCitationResult> citations, String modelName, String promptVersion) {

    public static ChatAnswerResult from(ChatAnswer answer) {
        List<ChatCitationResult> citations =
                answer.citations().stream().map(ChatCitationResult::from).toList();
        return new ChatAnswerResult(
                answer.answer(), citations, answer.modelName(), answer.promptVersion());
    }
}
