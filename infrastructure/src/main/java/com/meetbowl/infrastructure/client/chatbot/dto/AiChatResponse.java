package com.meetbowl.infrastructure.client.chatbot.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * meetbowl-ai 챗봇 API의 성공 응답 envelope다.
 *
 * <p>AI 서버 계약이 확장되어도 BE가 깨지지 않도록 알 수 없는 필드는 무시한다. 단, 응답을 그대로 신뢰하지 않고 adapter에서 도메인 스키마로 다시 검증한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiChatResponse(boolean success, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String answer, List<Source> sources, String model, String promptVersion) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
            String type, String resourceId, String title, String snippet, Double score) {}
}
