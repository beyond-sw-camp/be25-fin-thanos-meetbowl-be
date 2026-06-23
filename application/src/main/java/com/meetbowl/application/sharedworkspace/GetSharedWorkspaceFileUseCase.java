package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;

/**
 * 공유 자료 단건 메타데이터를 조회한다.
 *
 * <p>원본 파일 다운로드와 달리 JSON 메타데이터만 반환하지만, 같은 권한 경계를 적용해 워크스페이스 멤버만 파일 정보를 볼 수 있게 한다.
 */
@Service
public class GetSharedWorkspaceFileUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public GetSharedWorkspaceFileUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public SharedWorkspaceFileResult execute(UUID workspaceId, UUID fileId, UUID userId) {
        accessGuard.requireActiveMember(workspaceId, userId);
        SharedWorkspaceFile file =
                fileRepositoryPort
                        .findById(fileId)
                        .filter(found -> !found.isDeleted())
                        .filter(found -> found.workspaceId().equals(workspaceId))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));
        return SharedWorkspaceFileResult.from(file);
    }
}
