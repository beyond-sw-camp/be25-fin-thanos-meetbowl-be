package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRole;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "shared_workspace_members",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_shared_workspace_member",
                        columnNames = {"workspace_id", "user_id"}),
        indexes = {
            @Index(name = "idx_shared_workspace_member_user", columnList = "user_id"),
            @Index(name = "idx_shared_workspace_member_workspace", columnList = "workspace_id")
        })
public class SharedWorkspaceMemberEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID workspaceId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SharedWorkspaceMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SharedWorkspaceMemberStatus status;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID invitedByUserId;

    @Column(nullable = false)
    private Instant joinedAt;

    @Column private Instant removedAt;

    protected SharedWorkspaceMemberEntity() {}

    private SharedWorkspaceMemberEntity(
            UUID workspaceId,
            UUID userId,
            SharedWorkspaceMemberRole role,
            SharedWorkspaceMemberStatus status,
            UUID invitedByUserId,
            Instant joinedAt,
            Instant removedAt) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.status = status;
        this.invitedByUserId = invitedByUserId;
        this.joinedAt = joinedAt;
        this.removedAt = removedAt;
    }

    static SharedWorkspaceMemberEntity from(SharedWorkspaceMember member) {
        SharedWorkspaceMemberEntity entity =
                new SharedWorkspaceMemberEntity(
                        member.workspaceId(),
                        member.userId(),
                        member.role(),
                        member.status(),
                        member.invitedByUserId(),
                        member.joinedAt(),
                        member.removedAt());
        entity.setId(member.id());
        return entity;
    }

    SharedWorkspaceMember toDomain() {
        return SharedWorkspaceMember.of(
                getId(), workspaceId, userId, role, status, invitedByUserId, joinedAt, removedAt);
    }
}
