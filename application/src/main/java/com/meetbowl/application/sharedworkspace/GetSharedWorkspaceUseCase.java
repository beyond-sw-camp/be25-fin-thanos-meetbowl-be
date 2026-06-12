package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;

/** 공유 워크스페이스 상세를 조회한다. 멤버 또는 전 직원 공개 범위 접근만 허용하는 판정은 AccessGuard가 담당한다. */
@Service
public class GetSharedWorkspaceUseCase {

    private final SharedWorkspaceAccessGuard accessGuard;

    public GetSharedWorkspaceUseCase(SharedWorkspaceAccessGuard accessGuard) {
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public SharedWorkspaceResult execute(UUID workspaceId, UUID userId, UUID organizationId) {
        SharedWorkspace workspace =
                accessGuard.requireReadable(workspaceId, userId, organizationId);
        return SharedWorkspaceResult.from(workspace);
    }
}
