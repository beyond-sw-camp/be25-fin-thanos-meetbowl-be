package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

/**
 * 공유 자료를 업로드한다. 멤버만 가능하다. 최초 등록 시 파일 메타데이터와 v.1.0.0 버전 이력을 같은 트랜잭션에서 함께 만들어, 이후 모든 버전이 동일한 이력 테이블에서
 * 일관되게 추적되도록 한다.
 */
@Service
public class UploadSharedWorkspaceFileUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final Clock clock;

    public UploadSharedWorkspaceFileUseCase(
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
    public SharedWorkspaceFileResult execute(UploadSharedWorkspaceFileCommand command) {
        accessGuard.requireActiveMember(command.workspaceId(), command.uploaderUserId());

        Instant now = Instant.now(clock);
        SharedWorkspaceFile file =
                fileRepositoryPort.save(
                        SharedWorkspaceFile.create(
                                command.workspaceId(),
                                command.uploaderUserId(),
                                command.originalFileName(),
                                command.contentType(),
                                command.sizeBytes(),
                                command.storageKey(),
                                now));

        versionRepositoryPort.save(
                SharedWorkspaceFileVersion.create(
                        file.id(),
                        file.currentVersion(),
                        command.uploaderUserId(),
                        command.originalFileName(),
                        command.contentType(),
                        command.sizeBytes(),
                        command.storageKey(),
                        null,
                        now));

        return SharedWorkspaceFileResult.from(file);
    }
}
