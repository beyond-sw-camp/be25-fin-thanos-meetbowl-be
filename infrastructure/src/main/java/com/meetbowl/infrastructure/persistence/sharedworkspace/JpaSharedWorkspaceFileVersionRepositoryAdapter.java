package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

/**
 * 공유 파일 버전 이력의 {@link SharedWorkspaceFileVersionRepositoryPort}를 JPA로 구현한다.
 *
 * <p>버전 목록은 의미적 버전(major.minor.patch) 내림차순으로 돌려줘 최신 버전이 먼저 오게 한다. 새 버전이 올라와도 이전 버전 행을 지우지 않아 변경 이력이
 * 보존된다.
 */
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
        return repository
                .findByFileIdOrderByVersionMajorDescVersionMinorDescVersionPatchDesc(fileId)
                .stream()
                .map(SharedWorkspaceFileVersionEntity::toDomain)
                .toList();
    }
}
