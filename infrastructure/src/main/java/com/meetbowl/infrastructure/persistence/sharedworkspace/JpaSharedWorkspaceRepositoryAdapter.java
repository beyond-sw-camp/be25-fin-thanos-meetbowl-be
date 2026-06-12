package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceVisibility;

/**
 * 공유 워크스페이스의 {@link SharedWorkspaceRepositoryPort}를 JPA로 구현한다.
 *
 * <p>접근 가능한 워크스페이스 조회를 두 경로로 나눈다. 소유자 본인 소유분과, 같은 조직 전체 공개(ORGANIZATION) 워크스페이스다. 둘 다 삭제된 워크스페이스는
 * 제외해, 권한 계산이 살아 있는 워크스페이스만 보게 한다.
 */
@Repository
public class JpaSharedWorkspaceRepositoryAdapter implements SharedWorkspaceRepositoryPort {

    private final SpringDataSharedWorkspaceRepository repository;

    public JpaSharedWorkspaceRepositoryAdapter(SpringDataSharedWorkspaceRepository repository) {
        this.repository = repository;
    }

    @Override
    public SharedWorkspace save(SharedWorkspace workspace) {
        return repository.save(SharedWorkspaceEntity.from(workspace)).toDomain();
    }

    @Override
    public Optional<SharedWorkspace> findById(UUID workspaceId) {
        return repository.findById(workspaceId).map(SharedWorkspaceEntity::toDomain);
    }

    @Override
    public List<SharedWorkspace> findActiveByOwnerUserId(UUID ownerUserId) {
        return repository
                .findByOwnerUserIdAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(ownerUserId)
                .stream()
                .map(SharedWorkspaceEntity::toDomain)
                .toList();
    }

    @Override
    public List<SharedWorkspace> findOrganizationVisible(UUID organizationId) {
        return repository
                .findByOrganizationIdAndVisibilityAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(
                        organizationId, SharedWorkspaceVisibility.ORGANIZATION)
                .stream()
                .map(SharedWorkspaceEntity::toDomain)
                .toList();
    }
}
