package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;

/**
 * 공유 자료 메타데이터 응답 모델이다. currentVersion은 화면 표기 규칙(v.major.minor.patch)에 맞춘 문자열로 노출하고, 파일 원본 대신 저장 경로
 * 식별자만 전달한다.
 */
public record SharedWorkspaceFileResult(
        UUID fileId,
        UUID workspaceId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String currentVersion,
        Instant uploadedAt) {

    public static SharedWorkspaceFileResult from(SharedWorkspaceFile file) {
        return new SharedWorkspaceFileResult(
                file.id(),
                file.workspaceId(),
                file.uploaderUserId(),
                file.originalFileName(),
                file.contentType(),
                file.sizeBytes(),
                file.storageKey(),
                file.currentVersion().displayValue(),
                file.uploadedAt());
    }
}
