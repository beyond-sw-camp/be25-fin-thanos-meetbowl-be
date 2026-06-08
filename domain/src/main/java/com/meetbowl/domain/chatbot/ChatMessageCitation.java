package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class ChatMessageCitation {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SNIPPET_LENGTH = 2000;
    private static final int MAX_SOURCE_URI_LENGTH = 500;

    private final UUID id;
    private final UUID messageId;
    private final ChatSourceType sourceType;
    private final UUID sourceId;
    private final String title;
    private final String snippet;
    private final String sourceUri;
    private final Double score;
    private final int displayOrder;
    private final Instant citedAt;

    private ChatMessageCitation(
            UUID id,
            UUID messageId,
            ChatSourceType sourceType,
            UUID sourceId,
            String title,
            String snippet,
            String sourceUri,
            Double score,
            int displayOrder,
            Instant citedAt) {
        this.id = id;
        this.messageId = messageId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.snippet = snippet;
        this.sourceUri = sourceUri;
        this.score = score;
        this.displayOrder = displayOrder;
        this.citedAt = citedAt;
    }

    public static ChatMessageCitation create(
            UUID messageId,
            ChatSourceType sourceType,
            UUID sourceId,
            String title,
            String snippet,
            String sourceUri,
            Double score,
            int displayOrder,
            Instant citedAt) {
        return of(
                null,
                messageId,
                sourceType,
                sourceId,
                title,
                snippet,
                sourceUri,
                score,
                displayOrder,
                citedAt);
    }

    public static ChatMessageCitation of(
            UUID id,
            UUID messageId,
            ChatSourceType sourceType,
            UUID sourceId,
            String title,
            String snippet,
            String sourceUri,
            Double score,
            int displayOrder,
            Instant citedAt) {
        ChatDomainValidators.requireId(messageId, "챗봇 답변 출처 메시지 ID는 필수입니다.");
        if (sourceType == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 답변 출처 유형은 필수입니다.");
        }
        ChatDomainValidators.requireId(sourceId, "챗봇 답변 출처 ID는 필수입니다.");
        ChatDomainValidators.requireText(
                title, MAX_TITLE_LENGTH, "챗봇 답변 출처 제목은 필수입니다.", "챗봇 답변 출처 제목은 255자 이하여야 합니다.");
        ChatDomainValidators.requireText(
                snippet, MAX_SNIPPET_LENGTH, "챗봇 답변 출처 근거는 필수입니다.", "챗봇 답변 출처 근거는 2000자 이하여야 합니다.");
        ChatDomainValidators.validateOptionalLength(
                sourceUri, MAX_SOURCE_URI_LENGTH, "챗봇 답변 출처 URI는 500자 이하여야 합니다.");
        if (score != null && (score < 0.0D || score > 1.0D)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "챗봇 답변 출처 점수는 0 이상 1 이하여야 합니다.");
        }
        if (displayOrder < 1) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "챗봇 답변 출처 표시 순서는 1 이상이어야 합니다.");
        }
        ChatDomainValidators.requireInstant(citedAt, "챗봇 답변 출처 생성 시각은 필수입니다.");

        return new ChatMessageCitation(
                id,
                messageId,
                sourceType,
                sourceId,
                title.trim(),
                snippet.trim(),
                ChatDomainValidators.normalize(sourceUri),
                score,
                displayOrder,
                citedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID messageId() {
        return messageId;
    }

    public ChatSourceType sourceType() {
        return sourceType;
    }

    public UUID sourceId() {
        return sourceId;
    }

    public String title() {
        return title;
    }

    public String snippet() {
        return snippet;
    }

    public String sourceUri() {
        return sourceUri;
    }

    public Double score() {
        return score;
    }

    public int displayOrder() {
        return displayOrder;
    }

    public Instant citedAt() {
        return citedAt;
    }
}
