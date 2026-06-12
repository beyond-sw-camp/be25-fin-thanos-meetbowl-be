package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.DocumentVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

/**
 * 기존 공유 자료에 새 버전을 추가한다. 동시에 두 명이 같은 파일을 수정하면 한쪽 변경이 사라질 수 있어, 파일 행을 잠금 조회한 뒤 요청의 기대 버전과 DB 현재 버전을
 * 다시 비교한다. 기대 버전이 어긋나면 도메인이 충돌로 막고, 호출자는 최신 버전을 확인한 후 재시도하도록 409로 안내받는다.
 */
@Service
public class AddSharedWorkspaceFileVersionUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final Clock clock;

    public AddSharedWorkspaceFileVersionUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            Clock clock) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.accessGuard = accessGuard;
        this.clock = clock;
    }

    @Transactional
    public SharedWorkspaceFileVersionResult execute(AddSharedWorkspaceFileVersionCommand command) {
        accessGuard.requireActiveMember(command.workspaceId(), command.uploaderUserId());

        DocumentVersion expected = DocumentVersion.parse(command.expectedCurrentVersion());
        DocumentVersion newVersion = DocumentVersion.parse(command.newVersion());

        // 잠금 조회로 현재 버전을 고정한 뒤에야 충돌 검사와 버전 증가가 직렬화된다.
        SharedWorkspaceFile file =
                fileRepositoryPort
                        .findByIdForUpdate(command.fileId())
                        .filter(found -> !found.isDeleted())
                        .filter(found -> found.workspaceId().equals(command.workspaceId()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));

        Instant now = Instant.now(clock);
        SharedWorkspaceFile updated =
                file.addVersion(
                        command.uploaderUserId(),
                        command.originalFileName(),
                        command.contentType(),
                        command.sizeBytes(),
                        command.storageKey(),
                        expected,
                        newVersion,
                        now);
        fileRepositoryPort.save(updated);

        SharedWorkspaceFileVersion version =
                versionRepositoryPort.save(
                        SharedWorkspaceFileVersion.create(
                                updated.id(),
                                newVersion,
                                command.uploaderUserId(),
                                command.originalFileName(),
                                command.contentType(),
                                command.sizeBytes(),
                                command.storageKey(),
                                command.changeMemo(),
                                now));
        return SharedWorkspaceFileVersionResult.from(version);
    }
}
