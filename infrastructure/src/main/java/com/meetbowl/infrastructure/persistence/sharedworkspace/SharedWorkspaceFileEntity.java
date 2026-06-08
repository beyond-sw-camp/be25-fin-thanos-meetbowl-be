package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "shared_workspace_files",
        indexes = @Index(name = "idx_shared_workspace_file_workspace", columnList = "workspace_id"))
public class SharedWorkspaceFileEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID workspaceId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID uploaderUserId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false)
    private int currentVersionNumber;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column private Instant deletedAt;

    protected SharedWorkspaceFileEntity() {}

    private SharedWorkspaceFileEntity(
            UUID workspaceId,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            int currentVersionNumber,
            Instant uploadedAt,
            Instant deletedAt) {
        this.workspaceId = workspaceId;
        this.uploaderUserId = uploaderUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.currentVersionNumber = currentVersionNumber;
        this.uploadedAt = uploadedAt;
        this.deletedAt = deletedAt;
    }

    static SharedWorkspaceFileEntity from(SharedWorkspaceFile file) {
        SharedWorkspaceFileEntity entity =
                new SharedWorkspaceFileEntity(
                        file.workspaceId(),
                        file.uploaderUserId(),
                        file.originalFileName(),
                        file.contentType(),
                        file.sizeBytes(),
                        file.storageKey(),
                        file.currentVersionNumber(),
                        file.uploadedAt(),
                        file.deletedAt());
        entity.setId(file.id());
        return entity;
    }

    SharedWorkspaceFile toDomain() {
        return SharedWorkspaceFile.of(
                getId(),
                workspaceId,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                currentVersionNumber,
                uploadedAt,
                deletedAt);
    }
}
