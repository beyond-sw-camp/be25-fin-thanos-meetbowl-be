package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberStatus;

/**
 * 공유 워크스페이스 멤버십의 {@link SharedWorkspaceMemberRepositoryPort}를 JPA로 구현한다.
 *
 * <p>목록 조회는 ACTIVE 멤버만 돌려줘, 추방·탈퇴로 비활성된 멤버가 권한 계산에 다시 끼지 않게 한다. 사용자 기준 활성 멤버십 조회는 챗봇의 접근 가능한 공유
 * 워크스페이스 재계산에도 쓰인다.
 */
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
