package com.meetbowl.application.personalworkspace.drive;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.storage.StoredObject;

/** 개인 드라이브 파일 다운로드 응답에 필요한 메타데이터와 원본 바이트다. */
public record DriveFileDownloadResult(
        UUID fileId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        Instant uploadedAt,
        byte[] content) {

    public static DriveFileDownloadResult from(
            PersonalWorkspaceDriveFile file, StoredObject storedObject) {
        return new DriveFileDownloadResult(
                file.id(),
                file.originalFileName(),
                resolveContentType(file, storedObject),
                storedObject.contentLength(),
                file.uploadedAt(),
                storedObject.content());
    }

    private static String resolveContentType(
            PersonalWorkspaceDriveFile file, StoredObject storedObject) {
        if (storedObject.contentType() != null && !storedObject.contentType().isBlank()) {
            return storedObject.contentType();
        }
        if (file.contentType() != null && !file.contentType().isBlank()) {
            return file.contentType();
        }
        return "application/octet-stream";
    }
}
