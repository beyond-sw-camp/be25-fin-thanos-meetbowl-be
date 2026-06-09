package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPersonalWorkspaceDriveFileRepository
        extends JpaRepository<PersonalWorkspaceDriveFileEntity, UUID> {

    List<PersonalWorkspaceDriveFileEntity> findByOwnerUserIdAndDeletedAtIsNullOrderByUploadedAtDesc(
            UUID ownerUserId);
}
