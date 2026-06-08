package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "shared_workspace_file_versions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_shared_workspace_file_version",
                        columnNames = {"file_id", "version_number"}),
        indexes = @Index(name = "idx_shared_workspace_file_version_file", columnList = "file_id"))
public class SharedWorkspaceFileVersionEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID fileId;

    @Column(nullable = false)
    private int versionNumber;

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

    @Column(length = 1000)
    private String changeMemo;

    @Column(nullable = false)
    private Instant uploadedAt;

    protected SharedWorkspaceFileVersionEntity() {}

    private SharedWorkspaceFileVersionEntity(
            UUID fileId,
            int versionNumber,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String changeMemo,
            Instant uploadedAt) {
        this.fileId = fileId;
        this.versionNumber = versionNumber;
        this.uploaderUserId = uploaderUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.changeMemo = changeMemo;
        this.uploadedAt = uploadedAt;
    }

    static SharedWorkspaceFileVersionEntity from(SharedWorkspaceFileVersion version) {
        SharedWorkspaceFileVersionEntity entity =
                new SharedWorkspaceFileVersionEntity(
                        version.fileId(),
                        version.versionNumber(),
                        version.uploaderUserId(),
                        version.originalFileName(),
                        version.contentType(),
                        version.sizeBytes(),
                        version.storageKey(),
                        version.changeMemo(),
                        version.uploadedAt());
        entity.setId(version.id());
        return entity;
    }

    SharedWorkspaceFileVersion toDomain() {
        return SharedWorkspaceFileVersion.of(
                getId(),
                fileId,
                versionNumber,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                changeMemo,
                uploadedAt);
    }
}
