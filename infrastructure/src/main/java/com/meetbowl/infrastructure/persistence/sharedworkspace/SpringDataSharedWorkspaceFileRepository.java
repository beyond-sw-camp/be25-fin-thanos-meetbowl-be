package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface SpringDataSharedWorkspaceFileRepository
        extends JpaRepository<SharedWorkspaceFileEntity, UUID> {

    List<SharedWorkspaceFileEntity> findByWorkspaceIdAndDeletedAtIsNullOrderByUploadedAtDesc(
            UUID workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<SharedWorkspaceFileEntity> findForUpdateById(UUID fileId);
}
