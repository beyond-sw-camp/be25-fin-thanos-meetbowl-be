package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupSourceType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * {@link PersonalWorkspaceBackup} 애그리거트를 저장하는 영속 엔티티다. (소유자, 출처 유형, 출처 ID) 유니크 제약으로 같은 원본의 중복 백업을
 * 막는다.
 */
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

    @Column(columnDefinition = "MEDIUMTEXT")
    private String body;

    @Column(nullable = false)
    private Instant backedUpAt;

    @OneToMany(mappedBy = "backup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonalWorkspaceBackupAttachmentEntity> attachments = new ArrayList<>();

    protected PersonalWorkspaceBackupEntity() {}

    private PersonalWorkspaceBackupEntity(
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            String body,
            Instant backedUpAt) {
        this.ownerUserId = ownerUserId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.summary = summary;
        this.body = body;
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
                        backup.body(),
                        backup.backedUpAt());
        entity.setId(backup.id());
        backup.attachments()
                .forEach(
                        attachment ->
                                entity.attachments.add(
                                        PersonalWorkspaceBackupAttachmentEntity.from(
                                                entity, attachment)));
        return entity;
    }

    PersonalWorkspaceBackup toDomain() {
        return PersonalWorkspaceBackup.of(
                getId(),
                ownerUserId,
                sourceType,
                sourceId,
                title,
                summary,
                body,
                attachments.stream()
                        .map(PersonalWorkspaceBackupAttachmentEntity::toDomain)
                        .toList(),
                backedUpAt);
    }
}
