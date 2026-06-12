package com.meetbowl.api.sharedworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFileVersionResult;

/** 공유 자료 버전 이력 응답 DTO다. */
public record SharedWorkspaceFileVersionResponse(
        UUID versionId,
        UUID fileId,
        String version,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String changeMemo,
        Instant uploadedAt) {

    public static SharedWorkspaceFileVersionResponse from(SharedWorkspaceFileVersionResult result) {
        return new SharedWorkspaceFileVersionResponse(
                result.versionId(),
                result.fileId(),
                result.version(),
                result.uploaderUserId(),
                result.originalFileName(),
                result.contentType(),
                result.sizeBytes(),
                result.storageKey(),
                result.changeMemo(),
                result.uploadedAt());
    }
}
