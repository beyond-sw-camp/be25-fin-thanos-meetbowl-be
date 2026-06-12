package com.meetbowl.domain.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 공유 워크스페이스 애그리거트 루트다.
 *
 * <p>이름·설명 길이와 소유자·조직, 공개 범위를 관리한다. 기본 공개 범위는 멤버 전용(MEMBERS_ONLY)이며, 삭제는 행을 지우지 않고 삭제 시각을 남기는 soft
 * delete로 처리한다.
 */
public class SharedWorkspace {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final UUID id;
    private final UUID organizationId;
    private final UUID ownerUserId;
    private final String name;
    private final String description;
    private final SharedWorkspaceVisibility visibility;
    private final Instant createdAt;
    private final Instant deletedAt;

    private SharedWorkspace(
            UUID id,
            UUID organizationId,
            UUID ownerUserId,
            String name,
            String description,
            SharedWorkspaceVisibility visibility,
            Instant createdAt,
            Instant deletedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static SharedWorkspace create(
            UUID organizationId,
            UUID ownerUserId,
            String name,
            String description,
            Instant createdAt) {
        return of(
                null,
                organizationId,
                ownerUserId,
                name,
                description,
                SharedWorkspaceVisibility.MEMBERS_ONLY,
                createdAt,
                null);
    }

    public static SharedWorkspace of(
            UUID id,
            UUID organizationId,
            UUID ownerUserId,
            String name,
            String description,
            SharedWorkspaceVisibility visibility,
            Instant createdAt,
            Instant deletedAt) {
        SharedWorkspaceValidators.requireId(organizationId, "공유 워크스페이스 조직 ID는 필수입니다.");
        SharedWorkspaceValidators.requireId(ownerUserId, "공유 워크스페이스 소유자 ID는 필수입니다.");
        SharedWorkspaceValidators.requireText(
                name, MAX_NAME_LENGTH, "공유 워크스페이스 이름은 필수입니다.", "공유 워크스페이스 이름은 100자 이하여야 합니다.");
        SharedWorkspaceValidators.validateOptionalLength(
                description, MAX_DESCRIPTION_LENGTH, "공유 워크스페이스 설명은 1000자 이하여야 합니다.");
        if (visibility == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 공개 범위는 필수입니다.");
        }
        if (createdAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 생성 시각은 필수입니다.");
        }

        return new SharedWorkspace(
                id,
                organizationId,
                ownerUserId,
                name.trim(),
                SharedWorkspaceValidators.normalize(description),
                visibility,
                createdAt,
                deletedAt);
    }

    public SharedWorkspace update(String name, String description) {
        ensureActive();
        return of(
                id,
                organizationId,
                ownerUserId,
                name,
                description,
                visibility,
                createdAt,
                deletedAt);
    }

    public SharedWorkspace openToOrganization() {
        ensureActive();
        return of(
                id,
                organizationId,
                ownerUserId,
                name,
                description,
                SharedWorkspaceVisibility.ORGANIZATION,
                createdAt,
                deletedAt);
    }

    public SharedWorkspace restrictToMembers() {
        ensureActive();
        return of(
                id,
                organizationId,
                ownerUserId,
                name,
                description,
                SharedWorkspaceVisibility.MEMBERS_ONLY,
                createdAt,
                deletedAt);
    }

    public SharedWorkspace delete(Instant deletedAt) {
        ensureActive();
        if (deletedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 삭제 시각은 필수입니다.");
        }
        return of(
                id,
                organizationId,
                ownerUserId,
                name,
                description,
                visibility,
                createdAt,
                deletedAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId.equals(userId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isOrganizationVisible() {
        return visibility == SharedWorkspaceVisibility.ORGANIZATION;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public SharedWorkspaceVisibility visibility() {
        return visibility;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    private void ensureActive() {
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "삭제된 공유 워크스페이스는 변경할 수 없습니다.");
        }
    }
}
