package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "personal_workspace_drive_files")
public class PersonalWorkspaceDriveFileEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column private Instant deletedAt;

    protected PersonalWorkspaceDriveFileEntity() {}

    private PersonalWorkspaceDriveFileEntity(
            UUID ownerUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            Instant uploadedAt,
            Instant deletedAt) {
        this.ownerUserId = ownerUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.uploadedAt = uploadedAt;
        this.deletedAt = deletedAt;
    }

    static PersonalWorkspaceDriveFileEntity from(PersonalWorkspaceDriveFile file) {
        PersonalWorkspaceDriveFileEntity entity =
                new PersonalWorkspaceDriveFileEntity(
                        file.ownerUserId(),
                        file.originalFileName(),
                        file.contentType(),
                        file.sizeBytes(),
                        file.storageKey(),
                        file.uploadedAt(),
                        file.deletedAt());
        entity.setId(file.id());
        return entity;
    }

    PersonalWorkspaceDriveFile toDomain() {
        return PersonalWorkspaceDriveFile.of(
                getId(),
                ownerUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                uploadedAt,
                deletedAt);
    }
}
