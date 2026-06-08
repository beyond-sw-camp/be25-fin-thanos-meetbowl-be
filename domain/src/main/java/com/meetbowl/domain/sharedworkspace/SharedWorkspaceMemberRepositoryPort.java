package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedWorkspaceMemberRepositoryPort {

    SharedWorkspaceMember save(SharedWorkspaceMember member);

    Optional<SharedWorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<SharedWorkspaceMember> findActiveByWorkspaceId(UUID workspaceId);

    List<SharedWorkspaceMember> findActiveByUserId(UUID userId);
}
