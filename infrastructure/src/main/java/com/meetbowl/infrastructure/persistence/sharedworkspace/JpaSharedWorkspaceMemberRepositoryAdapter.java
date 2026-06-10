package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberStatus;

@Repository
public class JpaSharedWorkspaceMemberRepositoryAdapter
        implements SharedWorkspaceMemberRepositoryPort {

    private final SpringDataSharedWorkspaceMemberRepository repository;

    public JpaSharedWorkspaceMemberRepositoryAdapter(
            SpringDataSharedWorkspaceMemberRepository repository) {
        this.repository = repository;
    }

    @Override
    public SharedWorkspaceMember save(SharedWorkspaceMember member) {
        return repository.save(SharedWorkspaceMemberEntity.from(member)).toDomain();
    }

    @Override
    public Optional<SharedWorkspaceMember> findByWorkspaceIdAndUserId(
            UUID workspaceId, UUID userId) {
        return repository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(SharedWorkspaceMemberEntity::toDomain);
    }

    @Override
    public List<SharedWorkspaceMember> findActiveByWorkspaceId(UUID workspaceId) {
        return repository
                .findByWorkspaceIdAndStatusOrderByJoinedAtAsc(
                        workspaceId, SharedWorkspaceMemberStatus.ACTIVE)
                .stream()
                .map(SharedWorkspaceMemberEntity::toDomain)
                .toList();
    }

    @Override
    public List<SharedWorkspaceMember> findActiveByUserId(UUID userId) {
        return repository
                .findByUserIdAndStatusOrderByJoinedAtDesc(
                        userId, SharedWorkspaceMemberStatus.ACTIVE)
                .stream()
                .map(SharedWorkspaceMemberEntity::toDomain)
                .toList();
    }
}
