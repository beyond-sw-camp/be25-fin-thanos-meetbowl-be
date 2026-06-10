package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

@Repository
public class JpaPersonalWorkspaceBackupRepositoryAdapter
        implements PersonalWorkspaceBackupRepositoryPort {

    private final SpringDataPersonalWorkspaceBackupRepository repository;

    public JpaPersonalWorkspaceBackupRepositoryAdapter(
            SpringDataPersonalWorkspaceBackupRepository repository) {
        this.repository = repository;
    }

    @Override
    public PersonalWorkspaceBackup save(PersonalWorkspaceBackup backup) {
        return repository.save(PersonalWorkspaceBackupEntity.from(backup)).toDomain();
    }

    @Override
    public Optional<PersonalWorkspaceBackup> findById(UUID backupId) {
        return repository.findById(backupId).map(PersonalWorkspaceBackupEntity::toDomain);
    }

    @Override
    public List<PersonalWorkspaceBackup> findByOwnerUserId(UUID ownerUserId) {
        return repository.findByOwnerUserIdOrderByBackedUpAtDesc(ownerUserId).stream()
                .map(PersonalWorkspaceBackupEntity::toDomain)
                .toList();
    }

    @Override
    public List<PersonalWorkspaceBackup> searchByOwnerUserIdAndKeyword(
            UUID ownerUserId, String keyword) {
        return repository
                .findByOwnerUserIdAndTitleContainingIgnoreCaseOrderByBackedUpAtDesc(
                        ownerUserId, keyword)
                .stream()
                .map(PersonalWorkspaceBackupEntity::toDomain)
                .toList();
    }
}
