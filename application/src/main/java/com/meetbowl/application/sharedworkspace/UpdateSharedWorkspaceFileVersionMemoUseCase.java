package com.meetbowl.application.sharedworkspace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

/**
 * 특정 버전의 변경 메모만 수정한다. 메모는 원본 자료가 아니라 이력 설명이므로 저장 경로와 파일 본문은 그대로 둔다. versionId가 요청한 파일/워크스페이스에 실제로
 * 속하는지 확인해 경로 변수만 끼워 맞춘 접근을 차단한다.
 */
@Service
public class UpdateSharedWorkspaceFileVersionMemoUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public UpdateSharedWorkspaceFileVersionMemoUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public SharedWorkspaceFileVersionResult execute(
            UpdateSharedWorkspaceFileVersionMemoCommand command) {
        accessGuard.requireActiveMember(command.workspaceId(), command.requesterUserId());

        fileRepositoryPort
                .findById(command.fileId())
                .filter(found -> !found.isDeleted())
                .filter(found -> found.workspaceId().equals(command.workspaceId()))
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));

        SharedWorkspaceFileVersion version =
                versionRepositoryPort
                        .findById(command.versionId())
                        .filter(found -> found.fileId().equals(command.fileId()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_FILE_VERSION_NOT_FOUND));

        SharedWorkspaceFileVersion updated =
                versionRepositoryPort.save(version.updateChangeMemo(command.changeMemo()));
        return SharedWorkspaceFileVersionResult.from(updated);
    }
}
