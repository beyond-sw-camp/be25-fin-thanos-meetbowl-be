package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

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
    public Optional<PersonalWorkspaceMemo> findById(UUID memoId) {
        return repository.findById(memoId).map(PersonalWorkspaceMemoEntity::toDomain);
    }

    @Override
    public List<PersonalWorkspaceMemo> findByOwnerUserId(UUID ownerUserId) {
        return repository.findByOwnerUserIdOrderByMemoUpdatedAtDesc(ownerUserId).stream()
                .map(PersonalWorkspaceMemoEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID memoId) {
        repository.deleteById(memoId);
    }
}
