package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

@Service
public class AddSharedWorkspaceFileVersionUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final Clock clock;

    @Autowired
    public AddSharedWorkspaceFileVersionUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort) {
        this(fileRepositoryPort, versionRepositoryPort, Clock.systemUTC());
    }

    AddSharedWorkspaceFileVersionUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort,
            Clock clock) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public SharedWorkspaceFileResult execute(AddSharedWorkspaceFileVersionCommand command) {
        SharedWorkspaceFile currentFile =
                fileRepositoryPort
                        .findByIdForUpdate(command.fileId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND,
                                                "공유 워크스페이스 파일을 찾을 수 없습니다."));
        if (!currentFile.workspaceId().equals(command.workspaceId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "공유 워크스페이스 파일을 찾을 수 없습니다.");
        }

        Instant uploadedAt = Instant.now(clock);
        SharedWorkspaceFile updatedFile =
                currentFile.addVersion(
                        command.uploaderUserId(),
                        command.originalFileName(),
                        command.contentType(),
                        command.sizeBytes(),
                        command.storageKey(),
                        command.expectedCurrentVersion(),
                        command.newVersion(),
                        uploadedAt);

        versionRepositoryPort.save(
                SharedWorkspaceFileVersion.create(
                        currentFile.id(),
                        command.newVersion(),
                        command.uploaderUserId(),
                        command.originalFileName(),
                        command.contentType(),
                        command.sizeBytes(),
                        command.storageKey(),
                        command.changeMemo(),
                        uploadedAt));
        return SharedWorkspaceFileResult.from(fileRepositoryPort.save(updatedFile));
    }
}
