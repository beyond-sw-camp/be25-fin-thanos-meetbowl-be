package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupSourceType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "personal_workspace_backups",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_personal_workspace_backup_source",
                        columnNames = {"owner_user_id", "source_type", "source_id"}))
public class PersonalWorkspaceBackupEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PersonalWorkspaceBackupSourceType sourceType;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID sourceId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Column(nullable = false)
    private Instant backedUpAt;

    protected PersonalWorkspaceBackupEntity() {}

    private PersonalWorkspaceBackupEntity(
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            Instant backedUpAt) {
        this.ownerUserId = ownerUserId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.summary = summary;
        this.backedUpAt = backedUpAt;
    }

    static PersonalWorkspaceBackupEntity from(PersonalWorkspaceBackup backup) {
        PersonalWorkspaceBackupEntity entity =
                new PersonalWorkspaceBackupEntity(
                        backup.ownerUserId(),
                        backup.sourceType(),
                        backup.sourceId(),
                        backup.title(),
                        backup.summary(),
                        backup.backedUpAt());
        entity.setId(backup.id());
        return entity;
    }

    PersonalWorkspaceBackup toDomain() {
        return PersonalWorkspaceBackup.of(
                getId(), ownerUserId, sourceType, sourceId, title, summary, backedUpAt);
    }
}
