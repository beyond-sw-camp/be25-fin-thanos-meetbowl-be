package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.storage.StoredObject;

/** 공유 자료 다운로드 응답에 필요한 메타데이터와 원본 바이트다. */
public record SharedWorkspaceFileDownloadResult(
        UUID fileId,
        UUID workspaceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String currentVersion,
        Instant uploadedAt,
        byte[] content) {

    public static SharedWorkspaceFileDownloadResult from(
            SharedWorkspaceFile file, StoredObject storedObject) {
        return new SharedWorkspaceFileDownloadResult(
                file.id(),
                file.workspaceId(),
                file.originalFileName(),
                resolveContentType(file, storedObject),
                storedObject.contentLength(),
                file.currentVersion().displayValue(),
                file.uploadedAt(),
                storedObject.content());
    }

    private static String resolveContentType(SharedWorkspaceFile file, StoredObject storedObject) {
        if (storedObject.contentType() != null && !storedObject.contentType().isBlank()) {
            return storedObject.contentType();
        }
        if (file.contentType() != null && !file.contentType().isBlank()) {
            return file.contentType();
        }
        return "application/octet-stream";
    }
}
