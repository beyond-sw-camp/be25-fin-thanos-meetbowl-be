package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;

@Repository
public class JpaPersonalWorkspaceBackupBookmarkRepositoryAdapter
        implements PersonalWorkspaceBackupBookmarkRepositoryPort {

    private final SpringDataPersonalWorkspaceBackupBookmarkRepository repository;

    public JpaPersonalWorkspaceBackupBookmarkRepositoryAdapter(
            SpringDataPersonalWorkspaceBackupBookmarkRepository repository) {
        this.repository = repository;
    }

    @Override
    public PersonalWorkspaceBackupBookmark save(PersonalWorkspaceBackupBookmark bookmark) {
        return repository.save(PersonalWorkspaceBackupBookmarkEntity.from(bookmark)).toDomain();
    }

    @Override
    public Optional<PersonalWorkspaceBackupBookmark> findByOwnerUserIdAndBackupId(
            UUID ownerUserId, UUID backupId) {
        return repository
                .findByOwnerUserIdAndBackupId(ownerUserId, backupId)
                .map(PersonalWorkspaceBackupBookmarkEntity::toDomain);
    }

    @Override
    public List<PersonalWorkspaceBackupBookmark> findByOwnerUserId(UUID ownerUserId) {
        return repository.findByOwnerUserIdOrderByBookmarkedAtDesc(ownerUserId).stream()
                .map(PersonalWorkspaceBackupBookmarkEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByOwnerUserIdAndBackupId(UUID ownerUserId, UUID backupId) {
        repository.deleteByOwnerUserIdAndBackupId(ownerUserId, backupId);
    }
}
