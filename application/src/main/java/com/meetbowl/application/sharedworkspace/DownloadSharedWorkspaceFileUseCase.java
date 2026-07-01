package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.storage.ObjectStoragePort;

/**
 * 공유 자료 다운로드에 필요한 권한을 검증하고 원본 파일을 내려받는다.
 *
 * <p>워크스페이스 읽기 권한과 파일 소속 워크스페이스를 먼저 확인한다. 그 이후에만 storageKey로 Object Storage를 조회해, 권한 없는 사용자가 파일 저장
 * 경로를 통해 우회 다운로드하지 못하게 한다. 전 직원 공개 워크스페이스는 같은 조직 사용자에게 다운로드/미리보기를 허용한다.
 */
@Service
public class DownloadSharedWorkspaceFileUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final ObjectStoragePort objectStoragePort;

    public DownloadSharedWorkspaceFileUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            ObjectStoragePort objectStoragePort) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.accessGuard = accessGuard;
        this.objectStoragePort = objectStoragePort;
    }

    @Transactional(readOnly = true)
    public SharedWorkspaceFileDownloadResult execute(
            UUID workspaceId, UUID fileId, UUID userId, UUID organizationId) {
        accessGuard.requireReadable(workspaceId, userId, organizationId);
        SharedWorkspaceFile file = loadActiveFileInWorkspace(workspaceId, fileId);
        return SharedWorkspaceFileDownloadResult.from(
                file, objectStoragePort.download(file.storageKey()));
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
