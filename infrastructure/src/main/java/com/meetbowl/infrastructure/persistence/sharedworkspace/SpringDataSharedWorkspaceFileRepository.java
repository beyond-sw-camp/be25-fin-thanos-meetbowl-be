package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSharedWorkspaceFileRepository
        extends JpaRepository<SharedWorkspaceFileEntity, UUID> {

    List<SharedWorkspaceFileEntity> findByWorkspaceIdAndDeletedAtIsNullOrderByUploadedAtDesc(
            UUID workspaceId);
}
