package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;

/**
 * 공유 자료를 삭제한다. 버전 이력과 다른 멤버의 참조를 보존하기 위해 deletedAt 기반 soft delete로 처리한다. 자료를 올린 본인 또는 워크스페이스 소유자만
 * 지울 수 있어, 임의 멤버가 남의 자료를 제거하지 못하도록 업로더와 소유자 두 경우만 통과시킨다.
 */
@Service
public class DeleteSharedWorkspaceFileUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final Clock clock;

    public DeleteSharedWorkspaceFileUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            Clock clock) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.accessGuard = accessGuard;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID workspaceId, UUID fileId, UUID requesterUserId) {
        SharedWorkspaceMember member =
                accessGuard.requireActiveMember(workspaceId, requesterUserId);

        SharedWorkspaceFile file =
                fileRepositoryPort
                        .findById(fileId)
                        .filter(found -> !found.isDeleted())
                        .filter(found -> found.workspaceId().equals(workspaceId))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));

        if (!member.isOwner() && !file.uploaderUserId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.SHARED_WORKSPACE_FORBIDDEN);
        }

        fileRepositoryPort.save(file.delete(Instant.now(clock)));
    }
}
