package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/**
 * 공유 자료 업로드 입력이다. 파일 원본은 Object Storage에 먼저 저장된 뒤 그 저장 경로(storageKey)와 메타데이터만 전달받는다. DB에는 원본을 두지
 * 않는다는 규칙을 입력 단계에서부터 강제하는 형태다.
 */
public record UploadSharedWorkspaceFileCommand(
        UUID workspaceId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey) {}
