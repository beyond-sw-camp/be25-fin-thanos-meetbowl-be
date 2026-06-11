package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/**
 * 공유 워크스페이스를 삭제한다. 자료와 버전 이력을 보존해야 하므로 물리 삭제가 아니라 deletedAt 기반 soft delete로 처리하고, 멤버 행은 그대로 두어 이후
 * 일반 조회에서만 제외되도록 한다.
 */
@Service
public class DeleteSharedWorkspaceUseCase {

    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final Clock clock;

    public DeleteSharedWorkspaceUseCase(
            SharedWorkspaceRepositoryPort workspaceRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            Clock clock) {
        this.workspaceRepositoryPort = workspaceRepositoryPort;
        this.accessGuard = accessGuard;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID workspaceId, UUID requesterUserId) {
        SharedWorkspace workspace = accessGuard.requireOwner(workspaceId, requesterUserId);
        workspaceRepositoryPort.save(workspace.delete(Instant.now(clock)));
    }
}
