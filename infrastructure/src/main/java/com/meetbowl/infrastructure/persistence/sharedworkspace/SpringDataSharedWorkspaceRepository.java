package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceVisibility;

interface SpringDataSharedWorkspaceRepository extends JpaRepository<SharedWorkspaceEntity, UUID> {

    List<SharedWorkspaceEntity> findByOwnerUserIdAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(
            UUID ownerUserId);

    List<SharedWorkspaceEntity>
            findByOrganizationIdAndVisibilityAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(
                    UUID organizationId, SharedWorkspaceVisibility visibility);
}
