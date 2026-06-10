package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPersonalWorkspaceBackupBookmarkRepository
        extends JpaRepository<PersonalWorkspaceBackupBookmarkEntity, UUID> {

    Optional<PersonalWorkspaceBackupBookmarkEntity> findByOwnerUserIdAndBackupId(
            UUID ownerUserId, UUID backupId);

    List<PersonalWorkspaceBackupBookmarkEntity> findByOwnerUserIdOrderByBookmarkedAtDesc(
            UUID ownerUserId);

    void deleteByOwnerUserIdAndBackupId(UUID ownerUserId, UUID backupId);
}
