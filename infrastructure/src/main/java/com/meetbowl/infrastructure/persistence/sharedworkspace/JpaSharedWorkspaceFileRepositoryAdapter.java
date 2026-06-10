package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;

@Repository
public class JpaSharedWorkspaceFileRepositoryAdapter implements SharedWorkspaceFileRepositoryPort {

    private final SpringDataSharedWorkspaceFileRepository repository;

    public JpaSharedWorkspaceFileRepositoryAdapter(
            SpringDataSharedWorkspaceFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public SharedWorkspaceFile save(SharedWorkspaceFile file) {
        return repository.save(SharedWorkspaceFileEntity.from(file)).toDomain();
    }

    @Override
    public Optional<SharedWorkspaceFile> findById(UUID fileId) {
        return repository.findById(fileId).map(SharedWorkspaceFileEntity::toDomain);
    }

    @Override
    public Optional<SharedWorkspaceFile> findByIdForUpdate(UUID fileId) {
        return repository.findForUpdateById(fileId).map(SharedWorkspaceFileEntity::toDomain);
    }

    @Override
    public List<SharedWorkspaceFile> findActiveByWorkspaceId(UUID workspaceId) {
        return repository
                .findByWorkspaceIdAndDeletedAtIsNullOrderByUploadedAtDesc(workspaceId)
                .stream()
                .map(SharedWorkspaceFileEntity::toDomain)
                .toList();
    }
}
