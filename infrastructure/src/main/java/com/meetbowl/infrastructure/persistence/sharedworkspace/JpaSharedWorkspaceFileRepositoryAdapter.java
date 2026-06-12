package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;

/**
 * 공유 워크스페이스 파일 메타데이터의 {@link SharedWorkspaceFileRepositoryPort}를 JPA로 구현한다.
 *
 * <p>새 버전 추가처럼 동시성 충돌이 가능한 흐름을 위해 잠금 조회({@code findByIdForUpdate})를 별도로 제공한다. 목록은 삭제되지 않은 파일만 최신
 * 업로드순으로 돌려준다.
 */
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
