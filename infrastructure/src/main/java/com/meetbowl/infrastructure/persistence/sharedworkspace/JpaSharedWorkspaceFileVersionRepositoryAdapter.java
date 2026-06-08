package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

@Repository
public class JpaSharedWorkspaceFileVersionRepositoryAdapter
        implements SharedWorkspaceFileVersionRepositoryPort {

    private final SpringDataSharedWorkspaceFileVersionRepository repository;

    public JpaSharedWorkspaceFileVersionRepositoryAdapter(
            SpringDataSharedWorkspaceFileVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public SharedWorkspaceFileVersion save(SharedWorkspaceFileVersion version) {
        return repository.save(SharedWorkspaceFileVersionEntity.from(version)).toDomain();
    }

    @Override
    public Optional<SharedWorkspaceFileVersion> findById(UUID versionId) {
        return repository.findById(versionId).map(SharedWorkspaceFileVersionEntity::toDomain);
    }

    @Override
    public List<SharedWorkspaceFileVersion> findByFileId(UUID fileId) {
        return repository.findByFileIdOrderByVersionNumberDesc(fileId).stream()
                .map(SharedWorkspaceFileVersionEntity::toDomain)
                .toList();
    }
}
