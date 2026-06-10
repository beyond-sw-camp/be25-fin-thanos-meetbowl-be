package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

@Repository
public class JpaPersonalWorkspaceDriveFileRepositoryAdapter
        implements PersonalWorkspaceDriveFileRepositoryPort {

    private final SpringDataPersonalWorkspaceDriveFileRepository repository;

    public JpaPersonalWorkspaceDriveFileRepositoryAdapter(
            SpringDataPersonalWorkspaceDriveFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public PersonalWorkspaceDriveFile save(PersonalWorkspaceDriveFile file) {
        return repository.save(PersonalWorkspaceDriveFileEntity.from(file)).toDomain();
    }

    @Override
    public Optional<PersonalWorkspaceDriveFile> findById(UUID fileId) {
        return repository.findById(fileId).map(PersonalWorkspaceDriveFileEntity::toDomain);
    }

    @Override
    public List<PersonalWorkspaceDriveFile> findActiveByOwnerUserId(UUID ownerUserId) {
        return repository
                .findByOwnerUserIdAndDeletedAtIsNullOrderByUploadedAtDesc(ownerUserId)
                .stream()
                .map(PersonalWorkspaceDriveFileEntity::toDomain)
                .toList();
    }
}
