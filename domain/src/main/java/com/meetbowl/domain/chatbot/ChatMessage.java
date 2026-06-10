package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 하나의 챗봇 대화 세션에 기록되는 메시지다.
 *
 * <p>사용자 질문, AI 답변, 시스템 문맥을 같은 모델로 표현하며 {@code sequenceNumber}로 세션 안의 순서를 보장한다. 사용자 메시지만 {@code
 * senderUserId}를 가지며, AI 답변에는 재현성과 운영 추적을 위해 사용 모델, 프롬프트 버전, AI 요청 ID를 선택적으로 기록한다.
 *
 * <p>이 객체는 JPA와 무관한 순수 도메인 모델이다. MariaDB 저장은 Infrastructure의 {@code ChatMessageEntity}가 담당한다.
 */
public class ChatMessage {

    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_MODEL_NAME_LENGTH = 100;
    private static final int MAX_PROMPT_VERSION_LENGTH = 50;
    private static final int MAX_AI_REQUEST_ID_LENGTH = 100;

    /** 저장 전에는 null일 수 있으며 저장 후 MariaDB의 메시지 식별자를 가진다. */
    private final UUID id;

    /** 이 메시지가 속한 {@link ChatSession}의 ID다. */
    private final UUID sessionId;

    /** 메시지 작성 주체를 구분한다. */
    private final ChatMessageRole role;

    /** 사용자 메시지 작성자 ID다. ASSISTANT와 SYSTEM 메시지는 null이어야 한다. */
    private final UUID senderUserId;

    /** 세션 안에서 메시지를 정렬하는 1부터 시작하는 순번이다. */
    private final int sequenceNumber;

    /** 사용자 질문, AI 답변 또는 시스템 메시지의 본문이다. */
    private final String content;

    /** AI 답변 생성에 사용한 모델명이다. 사용자 메시지에는 저장하지 않는다. */
    private final String modelName;

    /** AI 답변 생성에 사용한 프롬프트 버전이다. */
    private final String promptVersion;

    /** meetbowl-ai 또는 외부 AI Provider 요청을 추적하기 위한 식별자다. */
    private final String aiRequestId;

    /** 메시지가 실제로 생성된 UTC 시각이다. JPA 감사용 createdAt과 구분한다. */
    private final Instant createdAt;

    private ChatMessage(
            UUID id,
            UUID sessionId,
            ChatMessageRole role,
            UUID senderUserId,
            int sequenceNumber,
            String content,
            String modelName,
            String promptVersion,
            String aiRequestId,
            Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.senderUserId = senderUserId;
        this.sequenceNumber = sequenceNumber;
        this.content = content;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.aiRequestId = aiRequestId;
        this.createdAt = createdAt;
    }

    /** 신규 사용자 질문을 생성한다. ID와 AI 추적 정보는 아직 없으므로 내부에서 null로 설정한다. */
    public static ChatMessage user(
            UUID sessionId,
            UUID senderUserId,
            int sequenceNumber,
            String content,
            Instant createdAt) {
        return of(
                null,
                sessionId,
                ChatMessageRole.USER,
                senderUserId,
                sequenceNumber,
                content,
                null,
                null,
                null,
                createdAt);
    }

    /** 신규 AI 답변을 생성한다. AI가 작성하므로 사용자 발신자 ID는 저장하지 않는다. */
    public static ChatMessage assistant(
            UUID sessionId,
            int sequenceNumber,
            String content,
            String modelName,
            String promptVersion,
            String aiRequestId,
            Instant createdAt) {
        return of(
                null,
                sessionId,
                ChatMessageRole.ASSISTANT,
                null,
                sequenceNumber,
                content,
                modelName,
                promptVersion,
                aiRequestId,
                createdAt);
    }

    /**
     * 저장된 메시지를 복원하거나 역할이 명시된 메시지를 생성한다.
     *
     * <p>JPA Entity의 {@code toDomain()}도 이 메서드를 사용하므로 신규 생성과 DB 복원에 동일한 도메인 불변식이 적용된다.
     */
    public static ChatMessage of(
            UUID id,
            UUID sessionId,
            ChatMessageRole role,
            UUID senderUserId,
            int sequenceNumber,
            String content,
            String modelName,
            String promptVersion,
            String aiRequestId,
            Instant createdAt) {
        ChatDomainValidators.requireId(sessionId, "챗봇 메시지 세션 ID는 필수입니다.");
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 메시지 역할은 필수입니다.");
        }
        if (role == ChatMessageRole.USER) {
            ChatDomainValidators.requireId(senderUserId, "사용자 챗봇 메시지는 발신자 ID가 필수입니다.");
        }
        if (role != ChatMessageRole.USER && senderUserId != null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "AI 또는 시스템 메시지는 발신자 ID를 가질 수 없습니다.");
        }
        if (sequenceNumber < 1) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "챗봇 메시지 순번은 1 이상이어야 합니다.");
        }
        ChatDomainValidators.requireText(
                content, MAX_CONTENT_LENGTH, "챗봇 메시지 내용은 필수입니다.", "챗봇 메시지 내용은 20000자 이하여야 합니다.");
        ChatDomainValidators.validateOptionalLength(
                modelName, MAX_MODEL_NAME_LENGTH, "AI 모델명은 100자 이하여야 합니다.");
        ChatDomainValidators.validateOptionalLength(
                promptVersion, MAX_PROMPT_VERSION_LENGTH, "프롬프트 버전은 50자 이하여야 합니다.");
        ChatDomainValidators.validateOptionalLength(
                aiRequestId, MAX_AI_REQUEST_ID_LENGTH, "AI 요청 ID는 100자 이하여야 합니다.");
        ChatDomainValidators.requireInstant(createdAt, "챗봇 메시지 생성 시각은 필수입니다.");

        return new ChatMessage(
                id,
                sessionId,
                role,
                senderUserId,
                sequenceNumber,
                content.trim(),
                ChatDomainValidators.normalize(modelName),
                ChatDomainValidators.normalize(promptVersion),
                ChatDomainValidators.normalize(aiRequestId),
                createdAt);
    }

    public boolean isFromUser() {
        return role == ChatMessageRole.USER;
    }

    public boolean isAssistantAnswer() {
        return role == ChatMessageRole.ASSISTANT;
    }

    public UUID id() {
        return id;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public ChatMessageRole role() {
        return role;
    }

    public UUID senderUserId() {
        return senderUserId;
    }

    public int sequenceNumber() {
        return sequenceNumber;
    }

    public String content() {
        return content;
    }

    public String modelName() {
        return modelName;
    }

    public String promptVersion() {
        return promptVersion;
    }

    public String aiRequestId() {
        return aiRequestId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
