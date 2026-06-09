package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberStatus;

interface SpringDataSharedWorkspaceMemberRepository
        extends JpaRepository<SharedWorkspaceMemberEntity, UUID> {

    Optional<SharedWorkspaceMemberEntity> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<SharedWorkspaceMemberEntity> findByWorkspaceIdAndStatusOrderByJoinedAtAsc(
            UUID workspaceId, SharedWorkspaceMemberStatus status);

    List<SharedWorkspaceMemberEntity> findByUserIdAndStatusOrderByJoinedAtDesc(
            UUID userId, SharedWorkspaceMemberStatus status);
}
