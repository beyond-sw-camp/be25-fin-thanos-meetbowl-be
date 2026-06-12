package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;

/**
 * 공유 자료 다운로드에 필요한 메타데이터를 조회한다. 실제 원본 스트림은 storageKey로 Object Storage에서 받아야 하므로, 이 UseCase는 권한 검증과
 * 저장 경로 확정까지만 책임진다. 권한 검증을 통과하지 못한 storageKey가 외부로 나가지 않게 하는 경계 지점이다.
 */
@Service
public class DownloadSharedWorkspaceFileUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public DownloadSharedWorkspaceFileUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public SharedWorkspaceFileResult execute(UUID workspaceId, UUID fileId, UUID userId) {
        accessGuard.requireActiveMember(workspaceId, userId);
        SharedWorkspaceFile file = loadActiveFileInWorkspace(workspaceId, fileId);
        return SharedWorkspaceFileResult.from(file);
    }

    private SharedWorkspaceFile loadActiveFileInWorkspace(UUID workspaceId, UUID fileId) {
        SharedWorkspaceFile file =
                fileRepositoryPort
                        .findById(fileId)
                        .filter(found -> !found.isDeleted())
                        .filter(found -> found.workspaceId().equals(workspaceId))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));
        return file;
    }
}
