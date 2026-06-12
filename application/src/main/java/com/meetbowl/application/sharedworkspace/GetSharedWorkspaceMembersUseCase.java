package com.meetbowl.application.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;

/** 공유 워크스페이스 멤버 목록을 조회한다. 멤버 또는 전 직원 공개 범위 접근자만 볼 수 있다. */
@Service
public class GetSharedWorkspaceMembersUseCase {

    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public GetSharedWorkspaceMembersUseCase(
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.memberRepositoryPort = memberRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<SharedWorkspaceMemberResult> execute(
            UUID workspaceId, UUID userId, UUID organizationId) {
        accessGuard.requireReadable(workspaceId, userId, organizationId);
        return memberRepositoryPort.findActiveByWorkspaceId(workspaceId).stream()
                .map(SharedWorkspaceMemberResult::from)
                .toList();
    }
}
