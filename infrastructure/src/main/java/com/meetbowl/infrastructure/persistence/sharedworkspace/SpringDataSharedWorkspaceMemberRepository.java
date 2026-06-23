package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberStatus;

/** 공유 워크스페이스 멤버십 엔티티의 Spring Data JPA 리포지토리다. 목록은 ACTIVE 상태 멤버만 조회해 권한 계산 기준을 맞춘다. */
interface SpringDataSharedWorkspaceMemberRepository
        extends JpaRepository<SharedWorkspaceMemberEntity, UUID> {

    Optional<SharedWorkspaceMemberEntity> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<SharedWorkspaceMemberEntity> findByWorkspaceIdAndStatusOrderByJoinedAtAsc(
            UUID workspaceId, SharedWorkspaceMemberStatus status);

    List<SharedWorkspaceMemberEntity> findByUserIdAndStatusOrderByJoinedAtDesc(
            UUID userId, SharedWorkspaceMemberStatus status);
}
