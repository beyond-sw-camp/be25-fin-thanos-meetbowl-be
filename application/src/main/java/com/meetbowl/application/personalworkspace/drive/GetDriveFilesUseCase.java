package com.meetbowl.application.personalworkspace.drive;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/** soft delete된 파일은 port 조회 단계에서 제외되므로 활성 파일만 반환된다. */
@Service
public class GetDriveFilesUseCase {

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;

    public GetDriveFilesUseCase(PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<DriveFileResult> execute(UUID userId) {
        return driveFileRepositoryPort.findActiveByOwnerUserId(userId).stream()
                .map(DriveFileResult::from)
                .toList();
    }
}
