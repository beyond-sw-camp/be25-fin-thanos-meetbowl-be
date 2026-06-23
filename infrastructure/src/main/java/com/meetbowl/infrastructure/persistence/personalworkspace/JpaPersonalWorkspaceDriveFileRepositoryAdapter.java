package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/**
 * 개인 드라이브 파일 메타데이터의 {@link PersonalWorkspaceDriveFileRepositoryPort}를 JPA로 구현한다.
 *
 * <p>파일 원본은 Object Storage에 두고 DB에는 메타데이터만 저장한다. 목록 조회는 삭제되지 않은 파일만 최신 업로드순으로 돌려준다.
 */
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
