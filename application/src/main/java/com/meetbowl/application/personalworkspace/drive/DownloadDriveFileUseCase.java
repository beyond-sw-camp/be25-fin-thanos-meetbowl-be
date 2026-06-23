package com.meetbowl.application.personalworkspace.drive;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;
import com.meetbowl.domain.storage.ObjectStoragePort;

/**
 * 개인 드라이브 파일 원본을 다운로드한다.
 *
 * <p>파일 원본은 S3에 있으므로 DB 메타데이터로 소유자와 삭제 여부를 먼저 검증한 뒤, 검증된 storageKey만 Object Storage Port로 넘긴다.
 */
@Service
public class DownloadDriveFileUseCase {

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;
    private final ObjectStoragePort objectStoragePort;

    public DownloadDriveFileUseCase(
            PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort,
            ObjectStoragePort objectStoragePort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
        this.objectStoragePort = objectStoragePort;
    }

    @Transactional(readOnly = true)
    public DriveFileDownloadResult execute(UUID userId, UUID fileId) {
        PersonalWorkspaceDriveFile file =
                driveFileRepositoryPort
                        .findById(fileId)
                        .filter(found -> !found.isDeleted())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "파일을 찾을 수 없습니다."));

        if (!file.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "본인 파일만 접근할 수 있습니다.");
        }

        return DriveFileDownloadResult.from(file, objectStoragePort.download(file.storageKey()));
    }
}
