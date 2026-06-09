package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.DocumentVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;

public record SharedWorkspaceFileResult(
        UUID id,
        UUID workspaceId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        DocumentVersion currentVersion,
        Instant uploadedAt) {

    static SharedWorkspaceFileResult from(SharedWorkspaceFile file) {
        return new SharedWorkspaceFileResult(
                file.id(),
                file.workspaceId(),
                file.uploaderUserId(),
                file.originalFileName(),
                file.contentType(),
                file.sizeBytes(),
                file.storageKey(),
                file.currentVersion(),
                file.uploadedAt());
    }
}
