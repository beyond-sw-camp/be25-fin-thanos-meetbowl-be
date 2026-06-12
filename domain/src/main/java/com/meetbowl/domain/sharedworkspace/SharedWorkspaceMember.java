package com.meetbowl.domain.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 공유 워크스페이스 멤버십 애그리거트다.
 *
 * <p>워크스페이스와 사용자, 역할(OWNER/MEMBER)과 상태(ACTIVE/REMOVED)를 잇는다. 추방·탈퇴는 상태를 REMOVED로 바꾸고 제거 시각을 남기며,
 * 재초대 시 같은 행을 다시 활성화해 권한 판정을 멤버십 단일 기준으로 유지한다.
 */
public class SharedWorkspaceMember {

    private final UUID id;
    private final UUID workspaceId;
    private final UUID userId;
    private final SharedWorkspaceMemberRole role;
    private final SharedWorkspaceMemberStatus status;
    private final UUID invitedByUserId;
    private final Instant joinedAt;
    private final Instant removedAt;

    private SharedWorkspaceMember(
            UUID id,
            UUID workspaceId,
            UUID userId,
            SharedWorkspaceMemberRole role,
            SharedWorkspaceMemberStatus status,
            UUID invitedByUserId,
            Instant joinedAt,
            Instant removedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.status = status;
        this.invitedByUserId = invitedByUserId;
        this.joinedAt = joinedAt;
        this.removedAt = removedAt;
    }

    public static SharedWorkspaceMember owner(
            UUID workspaceId, UUID ownerUserId, Instant joinedAt) {
        return of(
                null,
                workspaceId,
                ownerUserId,
                SharedWorkspaceMemberRole.OWNER,
                SharedWorkspaceMemberStatus.ACTIVE,
                ownerUserId,
                joinedAt,
                null);
    }

    public static SharedWorkspaceMember invite(
            UUID workspaceId, UUID userId, UUID invitedByUserId, Instant joinedAt) {
        return of(
                null,
                workspaceId,
                userId,
                SharedWorkspaceMemberRole.MEMBER,
                SharedWorkspaceMemberStatus.ACTIVE,
                invitedByUserId,
                joinedAt,
                null);
    }

    public static SharedWorkspaceMember of(
            UUID id,
            UUID workspaceId,
            UUID userId,
            SharedWorkspaceMemberRole role,
            SharedWorkspaceMemberStatus status,
            UUID invitedByUserId,
            Instant joinedAt,
            Instant removedAt) {
        SharedWorkspaceValidators.requireId(workspaceId, "공유 워크스페이스 ID는 필수입니다.");
        SharedWorkspaceValidators.requireId(userId, "공유 워크스페이스 멤버 사용자 ID는 필수입니다.");
        SharedWorkspaceValidators.requireId(invitedByUserId, "공유 워크스페이스 초대자 ID는 필수입니다.");
        if (role == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 멤버 역할은 필수입니다.");
        }
        if (status == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 멤버 상태는 필수입니다.");
        }
        if (joinedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 참여 시각은 필수입니다.");
        }
        if (status == SharedWorkspaceMemberStatus.REMOVED && removedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 멤버 제거 시각은 필수입니다.");
        }
        if (status == SharedWorkspaceMemberStatus.ACTIVE && removedAt != null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "활성 멤버는 제거 시각을 가질 수 없습니다.");
        }
        if (role == SharedWorkspaceMemberRole.OWNER
                && status != SharedWorkspaceMemberStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 워크스페이스 소유자는 활성 상태여야 합니다.");
        }

        return new SharedWorkspaceMember(
                id, workspaceId, userId, role, status, invitedByUserId, joinedAt, removedAt);
    }

    public SharedWorkspaceMember remove(Instant removedAt) {
        if (role == SharedWorkspaceMemberRole.OWNER) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "공유 워크스페이스 소유자는 멤버에서 제거할 수 없습니다.");
        }
        return of(
                id,
                workspaceId,
                userId,
                role,
                SharedWorkspaceMemberStatus.REMOVED,
                invitedByUserId,
                joinedAt,
                removedAt);
    }

    public SharedWorkspaceMember reactivate(UUID invitedByUserId, Instant rejoinedAt) {
        if (isActive()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 참여 중인 멤버입니다.");
        }
        return of(
                id,
                workspaceId,
                userId,
                SharedWorkspaceMemberRole.MEMBER,
                SharedWorkspaceMemberStatus.ACTIVE,
                invitedByUserId,
                rejoinedAt,
                null);
    }

    public boolean isActive() {
        return status == SharedWorkspaceMemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == SharedWorkspaceMemberRole.OWNER;
    }

    public UUID id() {
        return id;
    }

    public UUID workspaceId() {
        return workspaceId;
    }

    public UUID userId() {
        return userId;
    }

    public SharedWorkspaceMemberRole role() {
        return role;
    }

    public SharedWorkspaceMemberStatus status() {
        return status;
    }

    public UUID invitedByUserId() {
        return invitedByUserId;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public Instant removedAt() {
        return removedAt;
    }
}
