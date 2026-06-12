package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;

/**
 * 백업 자료 북마크의 {@link PersonalWorkspaceBackupBookmarkRepositoryPort}를 JPA로 구현한다.
 *
 * <p>(소유자, 백업) 단위로 조회·삭제해 다른 사용자의 북마크에 영향을 주지 않는다. 단건 존재 조회를 제공해 같은 백업을 중복 북마크하지 않도록 UseCase가 판단할 수
 * 있게 한다.
 */
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
