package com.meetbowl.api.chatbot;

import java.util.List;
import java.util.UUID;

import com.meetbowl.application.chatbot.ChatAnswerResult;
import com.meetbowl.application.chatbot.ChatCitationResult;

/**
 * 챗봇 질의 API 응답 DTO다.
 *
 * <p>이 응답 역시 서버에 저장되지 않으며 프론트엔드의 현재 화면 메모리에서만 유지된다. 출처는 답변 근거를 짧게 제시하기 위한 것이고, 원문 전체를 다시 노출하지 않는다.
 */
public record ChatMessageResponse(String answer, List<Source> sources, String model) {

    public record Source(
            String type,
            UUID sourceId,
            String title,
            String snippet,
            String sourceUri,
            Double score,
            int displayOrder) {

        public static Source from(ChatCitationResult citation) {
            return new Source(
                    citation.sourceType(),
                    citation.sourceId(),
                    citation.title(),
                    citation.snippet(),
                    citation.sourceUri(),
                    citation.score(),
                    citation.displayOrder());
        }
    }

    public static ChatMessageResponse from(ChatAnswerResult result) {
        List<Source> sources = result.citations().stream().map(Source::from).toList();
        return new ChatMessageResponse(result.answer(), sources, result.modelName());
    }
}
