package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class ChatSession {

    private static final int MAX_TITLE_LENGTH = 100;

    private final UUID id;
    private final UUID ownerUserId;
    private final String title;
    private final ChatScopeType scopeType;
    private final UUID scopeId;
    private final ChatSessionStatus status;
    private final Instant startedAt;
    private final Instant lastMessageAt;
    private final Instant deletedAt;

    private ChatSession(
            UUID id,
            UUID ownerUserId,
            String title,
            ChatScopeType scopeType,
            UUID scopeId,
            ChatSessionStatus status,
            Instant startedAt,
            Instant lastMessageAt,
            Instant deletedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.status = status;
        this.startedAt = startedAt;
        this.lastMessageAt = lastMessageAt;
        this.deletedAt = deletedAt;
    }

    public static ChatSession start(
            UUID ownerUserId,
            String title,
            ChatScopeType scopeType,
            UUID scopeId,
            Instant startedAt) {
        return of(
                null,
                ownerUserId,
                title,
                scopeType,
                scopeId,
                ChatSessionStatus.ACTIVE,
                startedAt,
                startedAt,
                null);
    }

    public static ChatSession of(
            UUID id,
            UUID ownerUserId,
            String title,
            ChatScopeType scopeType,
            UUID scopeId,
            ChatSessionStatus status,
            Instant startedAt,
            Instant lastMessageAt,
            Instant deletedAt) {
        ChatDomainValidators.requireId(ownerUserId, "챗봇 세션 소유자 ID는 필수입니다.");
        ChatDomainValidators.validateOptionalLength(
                title, MAX_TITLE_LENGTH, "챗봇 세션 제목은 100자 이하여야 합니다.");
        if (scopeType == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 세션 범위 유형은 필수입니다.");
        }
        if (scopeType == ChatScopeType.GENERAL && scopeId != null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "일반 챗봇 세션은 범위 ID를 가질 수 없습니다.");
        }
        if (scopeType != ChatScopeType.GENERAL && scopeId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 세션 범위 ID는 필수입니다.");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "챗봇 세션 상태는 필수입니다.");
        }
        ChatDomainValidators.requireInstant(startedAt, "챗봇 세션 시작 시각은 필수입니다.");
        ChatDomainValidators.requireInstant(lastMessageAt, "챗봇 세션 마지막 메시지 시각은 필수입니다.");
        if (lastMessageAt.isBefore(startedAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "마지막 메시지 시각은 세션 시작 시각보다 이전일 수 없습니다.");
        }
        if (status == ChatSessionStatus.DELETED && deletedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "삭제된 챗봇 세션은 삭제 시각이 필요합니다.");
        }
        if (status == ChatSessionStatus.ACTIVE && deletedAt != null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "활성 챗봇 세션은 삭제 시각을 가질 수 없습니다.");
        }

        return new ChatSession(
                id,
                ownerUserId,
                ChatDomainValidators.normalize(title),
                scopeType,
                scopeId,
                status,
                startedAt,
                lastMessageAt,
                deletedAt);
    }

    public ChatSession touch(Instant lastMessageAt) {
        return of(
                id,
                ownerUserId,
                title,
                scopeType,
                scopeId,
                status,
                startedAt,
                lastMessageAt,
                deletedAt);
    }

    public ChatSession rename(String title) {
        return of(
                id,
                ownerUserId,
                title,
                scopeType,
                scopeId,
                status,
                startedAt,
                lastMessageAt,
                deletedAt);
    }

    public ChatSession delete(Instant deletedAt) {
        return of(
                id,
                ownerUserId,
                title,
                scopeType,
                scopeId,
                ChatSessionStatus.DELETED,
                startedAt,
                lastMessageAt,
                deletedAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId.equals(userId);
    }

    public boolean isDeleted() {
        return status == ChatSessionStatus.DELETED;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String title() {
        return title;
    }

    public ChatScopeType scopeType() {
        return scopeType;
    }

    public UUID scopeId() {
        return scopeId;
    }

    public ChatSessionStatus status() {
        return status;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant lastMessageAt() {
        return lastMessageAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
