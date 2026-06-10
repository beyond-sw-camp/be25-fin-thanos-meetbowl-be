package com.meetbowl.application.personalworkspace.drive;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;

/**
 * 개인 드라이브 파일 UseCase의 출력 모델이다.
 *
 * <p>파일 원본은 DB가 아니라 Object Storage에 저장되므로 메타데이터와 저장 경로(storageKey)만 노출한다. 실제 다운로드 URL 발급은 스토리지 어댑터
 * 연동 후속 작업에서 storageKey를 기반으로 처리한다.
 */
public record DriveFileResult(
        UUID fileId,
        UUID ownerUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        Instant uploadedAt) {

    public static DriveFileResult from(PersonalWorkspaceDriveFile file) {
        return new DriveFileResult(
                file.id(),
                file.ownerUserId(),
                file.originalFileName(),
                file.contentType(),
                file.sizeBytes(),
                file.storageKey(),
                file.uploadedAt());
    }
}
