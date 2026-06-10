package com.meetbowl.application.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;

/** 공유 자료 목록을 조회한다. 자료 접근은 멤버 권한을 요구하며, 삭제된 자료는 Repository 조회에서 제외된다. */
@Service
public class GetSharedWorkspaceFilesUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public GetSharedWorkspaceFilesUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<SharedWorkspaceFileResult> execute(UUID workspaceId, UUID userId) {
        accessGuard.requireActiveMember(workspaceId, userId);
        return fileRepositoryPort.findActiveByWorkspaceId(workspaceId).stream()
                .map(SharedWorkspaceFileResult::from)
                .toList();
    }
}
