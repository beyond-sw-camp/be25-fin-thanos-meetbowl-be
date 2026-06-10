package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceVisibility;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "shared_workspaces",
        indexes = {
            @Index(name = "idx_shared_workspace_owner", columnList = "owner_user_id"),
            @Index(
                    name = "idx_shared_workspace_org_visibility",
                    columnList = "organization_id, visibility")
        })
public class SharedWorkspaceEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SharedWorkspaceVisibility visibility;

    @Column(nullable = false)
    private Instant workspaceCreatedAt;

    @Column private Instant deletedAt;

    protected SharedWorkspaceEntity() {}

    private SharedWorkspaceEntity(
            UUID organizationId,
            UUID ownerUserId,
            String name,
            String description,
            SharedWorkspaceVisibility visibility,
            Instant workspaceCreatedAt,
            Instant deletedAt) {
        this.organizationId = organizationId;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.workspaceCreatedAt = workspaceCreatedAt;
        this.deletedAt = deletedAt;
    }

    static SharedWorkspaceEntity from(SharedWorkspace workspace) {
        SharedWorkspaceEntity entity =
                new SharedWorkspaceEntity(
                        workspace.organizationId(),
                        workspace.ownerUserId(),
                        workspace.name(),
                        workspace.description(),
                        workspace.visibility(),
                        workspace.createdAt(),
                        workspace.deletedAt());
        entity.setId(workspace.id());
        return entity;
    }

    SharedWorkspace toDomain() {
        return SharedWorkspace.of(
                getId(),
                organizationId,
                ownerUserId,
                name,
                description,
                visibility,
                workspaceCreatedAt,
                deletedAt);
    }
}
