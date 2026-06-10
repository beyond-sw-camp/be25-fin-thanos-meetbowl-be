package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceVisibility;

@Repository
public class JpaSharedWorkspaceRepositoryAdapter implements SharedWorkspaceRepositoryPort {

    private final SpringDataSharedWorkspaceRepository repository;

    public JpaSharedWorkspaceRepositoryAdapter(SpringDataSharedWorkspaceRepository repository) {
        this.repository = repository;
    }

    @Override
    public SharedWorkspace save(SharedWorkspace workspace) {
        return repository.save(SharedWorkspaceEntity.from(workspace)).toDomain();
    }

    @Override
    public Optional<SharedWorkspace> findById(UUID workspaceId) {
        return repository.findById(workspaceId).map(SharedWorkspaceEntity::toDomain);
    }

    @Override
    public List<SharedWorkspace> findActiveByOwnerUserId(UUID ownerUserId) {
        return repository
                .findByOwnerUserIdAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(ownerUserId)
                .stream()
                .map(SharedWorkspaceEntity::toDomain)
                .toList();
    }

    @Override
    public List<SharedWorkspace> findOrganizationVisible(UUID organizationId) {
        return repository
                .findByOrganizationIdAndVisibilityAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(
                        organizationId, SharedWorkspaceVisibility.ORGANIZATION)
                .stream()
                .map(SharedWorkspaceEntity::toDomain)
                .toList();
    }
}
