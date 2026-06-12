package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;

/** 공유 자료 버전 이력 응답 모델이다. 버전 비교/표기 규칙을 화면이 다시 계산하지 않도록 표기 문자열로 노출한다. */
public record SharedWorkspaceFileVersionResult(
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

    public static SharedWorkspaceFileVersionResult from(SharedWorkspaceFileVersion version) {
        return new SharedWorkspaceFileVersionResult(
                version.id(),
                version.fileId(),
                version.version().displayValue(),
                version.uploaderUserId(),
                version.originalFileName(),
                version.contentType(),
                version.sizeBytes(),
                version.storageKey(),
                version.changeMemo(),
                version.uploadedAt());
    }
}
