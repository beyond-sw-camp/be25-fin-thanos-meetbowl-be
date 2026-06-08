package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSharedWorkspaceFileVersionRepository
        extends JpaRepository<SharedWorkspaceFileVersionEntity, UUID> {

    List<SharedWorkspaceFileVersionEntity> findByFileIdOrderByVersionNumberDesc(UUID fileId);
}
