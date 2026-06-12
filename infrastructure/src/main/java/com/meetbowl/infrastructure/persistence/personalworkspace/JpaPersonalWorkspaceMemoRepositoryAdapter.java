package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/**
 * 개인 메모의 {@link PersonalWorkspaceMemoRepositoryPort}를 JPA로 구현한다.
 *
 * <p>단건 조회·삭제를 소유자 ID와 함께 수행해, 다른 사용자의 메모를 ID만으로 읽거나 지우지 못하게 하는 소유권 경계를 쿼리 단계에서 강제한다.
 */
@Repository
public class JpaPersonalWorkspaceMemoRepositoryAdapter
        implements PersonalWorkspaceMemoRepositoryPort {

    private final SpringDataPersonalWorkspaceMemoRepository repository;

    public JpaPersonalWorkspaceMemoRepositoryAdapter(
            SpringDataPersonalWorkspaceMemoRepository repository) {
        this.repository = repository;
    }

    @Override
    public PersonalWorkspaceMemo save(PersonalWorkspaceMemo memo) {
        return repository.save(PersonalWorkspaceMemoEntity.from(memo)).toDomain();
    }

    @Override
    public Optional<PersonalWorkspaceMemo> findByIdAndOwnerUserId(UUID memoId, UUID ownerUserId) {
        return repository
                .findByIdAndOwnerUserId(memoId, ownerUserId)
                .map(PersonalWorkspaceMemoEntity::toDomain);
    }

    @Override
    public List<PersonalWorkspaceMemo> findByOwnerUserId(UUID ownerUserId) {
        return repository.findByOwnerUserIdOrderByMemoUpdatedAtDesc(ownerUserId).stream()
                .map(PersonalWorkspaceMemoEntity::toDomain)
                .toList();
    }

    @Override
    public boolean deleteByIdAndOwnerUserId(UUID memoId, UUID ownerUserId) {
        return repository.deleteByIdAndOwnerUserId(memoId, ownerUserId) > 0;
    }
}
