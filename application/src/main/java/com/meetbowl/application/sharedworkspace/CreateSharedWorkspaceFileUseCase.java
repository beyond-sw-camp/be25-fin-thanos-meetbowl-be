package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.DocumentVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

@Service
public class CreateSharedWorkspaceFileUseCase {

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final Clock clock;

    @Autowired
    public CreateSharedWorkspaceFileUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort) {
        this(fileRepositoryPort, versionRepositoryPort, Clock.systemUTC());
    }

    CreateSharedWorkspaceFileUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort,
            Clock clock) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public SharedWorkspaceFileResult execute(CreateSharedWorkspaceFileCommand command) {
        Instant uploadedAt = Instant.now(clock);
        SharedWorkspaceFile file =
                fileRepositoryPort.save(
                        SharedWorkspaceFile.create(
                                command.workspaceId(),
                                command.uploaderUserId(),
                                command.originalFileName(),
                                command.contentType(),
                                command.sizeBytes(),
                                command.storageKey(),
                                uploadedAt));

        versionRepositoryPort.save(
                SharedWorkspaceFileVersion.create(
                        file.id(),
                        DocumentVersion.INITIAL,
                        command.uploaderUserId(),
                        command.originalFileName(),
                        command.contentType(),
                        command.sizeBytes(),
                        command.storageKey(),
                        command.changeMemo(),
                        uploadedAt));
        return SharedWorkspaceFileResult.from(file);
    }
}
