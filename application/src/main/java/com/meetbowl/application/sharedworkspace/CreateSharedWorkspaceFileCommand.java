package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

public record CreateSharedWorkspaceFileCommand(
        UUID workspaceId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String changeMemo) {}
