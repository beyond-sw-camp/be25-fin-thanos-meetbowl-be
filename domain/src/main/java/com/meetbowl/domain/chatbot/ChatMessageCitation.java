package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * AI 답변이 근거로 사용한 원본 자료 한 건을 표현한다.
 *
 * <p>하나의 ASSISTANT 메시지는 여러 출처를 가질 수 있으며, 화면에서는 {@code displayOrder}에 따라 [1], [2] 형태로 표시할 수 있다. 원문
 * 전체를 복제하지 않고 자료 식별자, 제목, 답변 근거가 된 짧은 발췌문만 저장한다.
 *
 * <p>사용자의 자료 접근 권한은 이 객체에 저장하지 않는다. 권한은 질문 처리 및 기존 대화 조회 시점에 meetbowl-be가 다시 확인하며, 권한을 잃은 출처의
 * 제목·근거·링크는 API 응답 단계에서 마스킹한다.
 */
public class ChatMessageCitation {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SNIPPET_LENGTH = 2000;
    private static final int MAX_SOURCE_URI_LENGTH = 500;

    /** 저장된 출처 이력의 식별자다. 신규 생성 시에는 null일 수 있다. */
    private final UUID id;

    /** 이 출처를 사용한 ASSISTANT 메시지 ID다. */
    private final UUID messageId;

    /** 백업 메일, 회의록, 개인 메모 등 원본 자료의 종류다. */
    private final ChatSourceType sourceType;

    /** 답변 생성에 실제 사용된 원본 ID다. 공유 워크스페이스 자료는 파일 버전 ID를 저장한다. */
    private final UUID sourceId;

    /** 답변 생성 당시 사용자에게 표시할 원본 자료 제목이다. */
    private final String title;

    /** AI 답변의 근거가 된 짧은 원문 발췌다. 원본 전체를 저장하지 않는다. */
    private final String snippet;

    /** 원본 자료 상세 화면으로 이동할 수 있는 내부 URI다. 현재 권한 확인 후에만 노출한다. */
    private final String sourceUri;

    /** 벡터 검색 또는 재정렬 결과의 관련도 점수다. 없을 수 있으며 저장 시 0~1 범위만 허용한다. */
    private final Double score;

    /** 한 답변 안에서 출처를 표시하는 1부터 시작하는 순서다. */
    private final int displayOrder;

    /** 해당 자료가 답변 근거로 채택된 UTC 시각이다. */
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

    /** AI 답변 저장 과정에서 신규 출처 이력을 생성한다. */
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

    /** 신규 생성과 영속성 복원에 공통으로 적용되는 검증 팩토리다. */
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
