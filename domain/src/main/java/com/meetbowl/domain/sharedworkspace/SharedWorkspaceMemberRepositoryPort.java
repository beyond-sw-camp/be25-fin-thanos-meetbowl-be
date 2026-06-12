package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 공유 워크스페이스 멤버십 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 목록은 활성 멤버만 조회한다. */
public interface SharedWorkspaceMemberRepositoryPort {

    SharedWorkspaceMember save(SharedWorkspaceMember member);

    Optional<SharedWorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<SharedWorkspaceMember> findActiveByWorkspaceId(UUID workspaceId);

    List<SharedWorkspaceMember> findActiveByUserId(UUID userId);
}
