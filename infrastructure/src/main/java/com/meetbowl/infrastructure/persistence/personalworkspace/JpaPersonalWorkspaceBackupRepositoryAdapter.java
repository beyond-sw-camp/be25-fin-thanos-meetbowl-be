package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

/**
 * 개인 백업 자료의 {@link PersonalWorkspaceBackupRepositoryPort}를 JPA로 구현한다.
 *
 * <p>목록·검색을 소유자 ID로 한정하고 최신 백업순으로 돌려준다. 검색은 제목을 대소문자 구분 없이 매칭한다.
 */
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
