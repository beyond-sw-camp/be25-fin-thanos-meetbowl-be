package com.meetbowl.application.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

/** 공유 자료의 버전 이력을 조회한다. 자료가 대상 워크스페이스에 속하는지 확인해 다른 워크스페이스의 fileId로 이력이 새지 않도록 막는다. */
@Service
public class GetSharedWorkspaceFileVersionsUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public GetSharedWorkspaceFileVersionsUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<SharedWorkspaceFileVersionResult> execute(
            UUID workspaceId, UUID fileId, UUID userId) {
        accessGuard.requireActiveMember(workspaceId, userId);

        fileRepositoryPort
                .findById(fileId)
                .filter(found -> !found.isDeleted())
                .filter(found -> found.workspaceId().equals(workspaceId))
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));

        return versionRepositoryPort.findByFileId(fileId).stream()
                .map(SharedWorkspaceFileVersionResult::from)
                .toList();
    }
}
