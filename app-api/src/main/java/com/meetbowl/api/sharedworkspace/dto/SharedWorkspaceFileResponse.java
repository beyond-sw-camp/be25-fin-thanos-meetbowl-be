package com.meetbowl.api.sharedworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFileResult;

/** 공유 자료 메타데이터 응답 DTO다. 파일 원본은 내려가지 않고 저장 경로와 메타데이터만 노출한다. */
public record SharedWorkspaceFileResponse(
        UUID fileId,
        UUID workspaceId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String currentVersion,
        Instant uploadedAt) {

    public static SharedWorkspaceFileResponse from(SharedWorkspaceFileResult result) {
        return new SharedWorkspaceFileResponse(
                result.fileId(),
                result.workspaceId(),
                result.uploaderUserId(),
                result.originalFileName(),
                result.contentType(),
                result.sizeBytes(),
                result.storageKey(),
                result.currentVersion(),
                result.uploadedAt());
    }
}
