package com.meetbowl.domain.chatbot;

import java.util.List;

/**
 * 스키마 검증을 마친 AI 응답만 API 계층으로 전달하기 위한 도메인 결과다.
 *
 * <p>LLM의 자유 형식 응답을 그대로 노출하면 출처 누락이나 잘못된 식별자가 외부 계약으로 굳어질 수 있으므로, 답변과 출처 및 재현에 필요한 최소 모델 정보를 함께
 * 검증한다. 이 값 역시 응답 후 저장하지 않는다.
 */
public record ChatAnswer(
        String answer, List<ChatCitation> citations, String modelName, String promptVersion) {

    private static final int MAX_ANSWER_LENGTH = 20_000;
    private static final int MAX_CITATION_COUNT = 20;
    private static final int MAX_MODEL_NAME_LENGTH = 100;
    private static final int MAX_PROMPT_VERSION_LENGTH = 50;

    public ChatAnswer {
        answer =
                ChatDomainValidators.requireText(
                        answer, MAX_ANSWER_LENGTH, "챗봇 답변은 필수입니다.", "챗봇 답변은 20000자 이하여야 합니다.");
        citations = citations == null ? List.of() : citations;
        if (citations.size() > MAX_CITATION_COUNT) {
            throw ChatDomainValidators.invalid("챗봇 답변 출처는 최대 20개까지 포함할 수 있습니다.");
        }
        if (citations.stream().anyMatch(citation -> citation == null)) {
            throw ChatDomainValidators.invalid("챗봇 답변 출처에 빈 값을 포함할 수 없습니다.");
        }
        long distinctDisplayOrders =
                citations.stream().map(ChatCitation::displayOrder).distinct().count();
        if (distinctDisplayOrders != citations.size()) {
            throw ChatDomainValidators.invalid("챗봇 답변 출처 표시 순서는 중복될 수 없습니다.");
        }
        citations = List.copyOf(citations);
        modelName =
                ChatDomainValidators.requireText(
                        modelName,
                        MAX_MODEL_NAME_LENGTH,
                        "AI 모델명은 필수입니다.",
                        "AI 모델명은 100자 이하여야 합니다.");
        promptVersion =
                ChatDomainValidators.requireText(
                        promptVersion,
                        MAX_PROMPT_VERSION_LENGTH,
                        "프롬프트 버전은 필수입니다.",
                        "프롬프트 버전은 50자 이하여야 합니다.");
    }
}
