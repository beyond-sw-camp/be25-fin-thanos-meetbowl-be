package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class ChatMessage {

    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_MODEL_NAME_LENGTH = 100;
    private static final int MAX_PROMPT_VERSION_LENGTH = 50;
    private static final int MAX_AI_REQUEST_ID_LENGTH = 100;

    private final UUID id;
    private final UUID sessionId;
    private final ChatMessageRole role;
    private final UUID senderUserId;
    private final int sequenceNumber;
    private final String content;
    private final String modelName;
    private final String promptVersion;
    private final String aiRequestId;
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
