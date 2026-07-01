package com.meetbowl.application.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;

/** 공유 자료 목록을 조회한다. 전 직원 공개 워크스페이스는 같은 조직 사용자에게 읽기 권한을 허용한다. */
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
    public List<SharedWorkspaceFileResult> execute(
            UUID workspaceId, UUID userId, UUID organizationId) {
        accessGuard.requireReadable(workspaceId, userId, organizationId);
        return fileRepositoryPort.findActiveByWorkspaceId(workspaceId).stream()
                .map(SharedWorkspaceFileResult::from)
                .toList();
    }
}
