package com.meetbowl.application.personalworkspace.drive;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/**
 * 개인 드라이브 파일 메타데이터를 등록한다.
 *
 * <p>실제 바이너리는 Object Storage에 저장하고 DB에는 메타데이터만 남긴다. 저장 경로는 사용자별로 충돌하지 않도록 {@code
 * personal-drive/{userId}/{uuid}} 형식으로 서버가 생성한다. 바이너리 전송과 presigned URL 발급은 스토리지 어댑터 연동 후속 작업이다.
 */
@Service
public class RegisterDriveFileUseCase {

    private static final String STORAGE_KEY_PREFIX = "personal-drive/";

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;

    public RegisterDriveFileUseCase(
            PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
    }

    @Transactional
    public DriveFileResult execute(RegisterDriveFileCommand command) {
        String storageKey = generateStorageKey(command.ownerUserId());

        PersonalWorkspaceDriveFile file =
                PersonalWorkspaceDriveFile.create(
                        command.ownerUserId(),
                        command.originalFileName(),
                        command.contentType(),
                        command.sizeBytes(),
                        storageKey,
                        Instant.now());

        return DriveFileResult.from(driveFileRepositoryPort.save(file));
    }

    private String generateStorageKey(UUID ownerUserId) {
        return STORAGE_KEY_PREFIX + ownerUserId + "/" + UUID.randomUUID();
    }
}
