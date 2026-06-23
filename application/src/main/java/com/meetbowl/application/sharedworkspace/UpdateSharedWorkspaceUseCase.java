package com.meetbowl.application.sharedworkspace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/** 공유 워크스페이스 정보를 수정한다. 소유자만 변경할 수 있고, 도메인이 활성 상태와 입력값 제약을 다시 검증한다. */
@Service
public class UpdateSharedWorkspaceUseCase {

    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public UpdateSharedWorkspaceUseCase(
            SharedWorkspaceRepositoryPort workspaceRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.workspaceRepositoryPort = workspaceRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public SharedWorkspaceResult execute(UpdateSharedWorkspaceCommand command) {
        SharedWorkspace workspace =
                accessGuard.requireOwner(command.workspaceId(), command.requesterUserId());
        SharedWorkspace updated =
                workspaceRepositoryPort.save(
                        workspace.update(command.name(), command.description()));
        return SharedWorkspaceResult.from(updated);
    }
}
