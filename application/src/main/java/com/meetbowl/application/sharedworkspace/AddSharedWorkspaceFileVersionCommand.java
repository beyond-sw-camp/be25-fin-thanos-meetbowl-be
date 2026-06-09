package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.DocumentVersion;

public record AddSharedWorkspaceFileVersionCommand(
        UUID workspaceId,
        UUID fileId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String changeMemo,
        DocumentVersion expectedCurrentVersion,
        DocumentVersion newVersion) {}
