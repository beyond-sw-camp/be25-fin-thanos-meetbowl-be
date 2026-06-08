package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPersonalWorkspaceBackupRepository
        extends JpaRepository<PersonalWorkspaceBackupEntity, UUID> {

    List<PersonalWorkspaceBackupEntity> findByOwnerUserIdOrderByBackedUpAtDesc(UUID ownerUserId);

    List<PersonalWorkspaceBackupEntity>
            findByOwnerUserIdAndTitleContainingIgnoreCaseOrderByBackedUpAtDesc(
                    UUID ownerUserId, String keyword);
}
