package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 한 사용자가 챗봇과 이어가는 대화의 최상위 도메인 모델이다.
 *
 * <p>세션은 소유 사용자, 제목, 검색 범위, 상태와 마지막 활동 시각을 관리한다. 현재 정책에서는 자동 만료하지 않으며 사용자가 명시적으로 삭제하기 전까지 ACTIVE
 * 상태로 유지한다. 삭제는 이력 보존을 위한 soft delete 방식이다.
 *
 * <p>LLM에 전달할 과거 메시지 Window Size와 대화 요약 정책은 아직 이 모델의 책임으로 확정되지 않았다. 해당 정책이 확정되기 전에는 세션 컨텍스트 요약 정보를
 * 이 객체에 추가하지 않는다.
 */
public class ChatSession {

    private static final int MAX_TITLE_LENGTH = 100;

    /** 저장된 세션의 식별자다. 신규 세션 생성 시에는 null일 수 있다. */
    private final UUID id;

    /** 세션을 생성하고 조회·질문·삭제할 수 있는 사용자 ID다. */
    private final UUID ownerUserId;

    /** 대화 목록에 표시할 세션 제목이다. 제목이 정해지지 않은 경우 null일 수 있다. */
    private final String title;

    /** 전체 자료 또는 특정 회의·회의록·워크스페이스로 검색 범위를 제한하는 유형이다. */
    private final ChatScopeType scopeType;

    /** 제한된 검색 범위의 리소스 ID다. GENERAL 범위에서는 반드시 null이다. */
    private final UUID scopeId;

    /** 세션이 계속 사용 가능한지 또는 삭제됐는지를 나타낸다. */
    private final ChatSessionStatus status;

    /** 세션이 시작된 UTC 시각이다. */
    private final Instant startedAt;

    /** 마지막 메시지가 추가된 UTC 시각이며 세션 목록의 최근 활동 정렬에 사용한다. */
    private final Instant lastMessageAt;

    /** soft delete 시각이다. ACTIVE이면 null이고 DELETED이면 반드시 값이 있어야 한다. */
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

    /** 신규 활성 세션을 시작한다. 시작 시각을 최초 마지막 메시지 시각으로도 사용한다. */
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

    /** 신규 세션 검증과 DB에서 조회한 세션 복원에 공통으로 사용하는 팩토리다. */
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

    /** 새 메시지가 저장된 뒤 마지막 활동 시각을 갱신한 새 불변 객체를 반환한다. */
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

    /** 세션 제목만 변경한 새 불변 객체를 반환한다. */
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

    /** 세션을 물리 삭제하지 않고 DELETED 상태와 삭제 시각을 기록한다. */
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
