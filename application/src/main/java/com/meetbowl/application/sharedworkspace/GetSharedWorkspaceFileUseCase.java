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
 * <p>원본 파일 다운로드와 달리 JSON 메타데이터만 반환하지만, 같은 권한 경계를 적용한다. 전 직원 공개 워크스페이스는 같은 조직 사용자에게 읽기 권한을 허용한다.
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
    public SharedWorkspaceFileResult execute(
            UUID workspaceId, UUID fileId, UUID userId, UUID organizationId) {
        accessGuard.requireReadable(workspaceId, userId, organizationId);
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
