package com.meetbowl.domain.chatbot;

import java.util.UUID;

/**
 * 현재 AI 답변에 포함해 클라이언트로 반환할 출처다.
 *
 * <p>답변과 함께 폐기되는 값이므로 메시지 ID나 저장 시각을 갖지 않는다. 원문 전체 대신 짧은 근거만 허용해 챗봇 응답이 새로운 문서 저장소처럼 사용되는 것을 막는다.
 */
public record ChatCitation(
        ChatSourceType sourceType,
        UUID sourceId,
        String title,
        String snippet,
        String sourceUri,
        Double score,
        int displayOrder) {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SNIPPET_LENGTH = 2_000;
    private static final int MAX_SOURCE_URI_LENGTH = 500;

    public ChatCitation {
        if (sourceType == null) {
            throw ChatDomainValidators.invalid("챗봇 답변 출처 유형은 필수입니다.");
        }
        ChatDomainValidators.requireId(sourceId, "챗봇 답변 출처 ID는 필수입니다.");
        title =
                ChatDomainValidators.requireText(
                        title,
                        MAX_TITLE_LENGTH,
                        "챗봇 답변 출처 제목은 필수입니다.",
                        "챗봇 답변 출처 제목은 255자 이하여야 합니다.");
        snippet =
                ChatDomainValidators.requireText(
                        snippet,
                        MAX_SNIPPET_LENGTH,
                        "챗봇 답변 출처 근거는 필수입니다.",
                        "챗봇 답변 출처 근거는 2000자 이하여야 합니다.");
        sourceUri =
                ChatDomainValidators.normalizeOptional(
                        sourceUri, MAX_SOURCE_URI_LENGTH, "챗봇 답변 출처 URI는 500자 이하여야 합니다.");
        if (score != null && (score < 0.0D || score > 1.0D)) {
            throw ChatDomainValidators.invalid("챗봇 답변 출처 점수는 0 이상 1 이하여야 합니다.");
        }
        if (displayOrder < 1) {
            throw ChatDomainValidators.invalid("챗봇 답변 출처 표시 순서는 1 이상이어야 합니다.");
        }
    }
}
