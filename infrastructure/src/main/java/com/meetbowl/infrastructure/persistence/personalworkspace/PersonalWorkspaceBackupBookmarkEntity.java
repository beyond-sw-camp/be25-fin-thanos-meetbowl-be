package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "personal_workspace_backup_bookmarks",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_personal_workspace_backup_bookmark",
                        columnNames = {"owner_user_id", "backup_id"}))
public class PersonalWorkspaceBackupBookmarkEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID backupId;

    @Column(nullable = false)
    private Instant bookmarkedAt;

    protected PersonalWorkspaceBackupBookmarkEntity() {}

    private PersonalWorkspaceBackupBookmarkEntity(
            UUID ownerUserId, UUID backupId, Instant bookmarkedAt) {
        this.ownerUserId = ownerUserId;
        this.backupId = backupId;
        this.bookmarkedAt = bookmarkedAt;
    }

    static PersonalWorkspaceBackupBookmarkEntity from(PersonalWorkspaceBackupBookmark bookmark) {
        PersonalWorkspaceBackupBookmarkEntity entity =
                new PersonalWorkspaceBackupBookmarkEntity(
                        bookmark.ownerUserId(), bookmark.backupId(), bookmark.bookmarkedAt());
        entity.setId(bookmark.id());
        return entity;
    }

    PersonalWorkspaceBackupBookmark toDomain() {
        return PersonalWorkspaceBackupBookmark.of(getId(), ownerUserId, backupId, bookmarkedAt);
    }
}
