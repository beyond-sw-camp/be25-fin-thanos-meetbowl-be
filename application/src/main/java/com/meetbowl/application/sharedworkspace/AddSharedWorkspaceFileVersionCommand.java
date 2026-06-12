package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/**
 * 새 파일 버전 업로드 입력이다. expectedCurrentVersion은 클라이언트가 마지막으로 확인한 현재 버전으로, 동시 수정으로 인한 분실 갱신을 막는 낙관적 충돌
 * 검사에 쓴다. newVersion은 수정자가 직접 지정하는 major.minor.patch 값이다.
 */
public record AddSharedWorkspaceFileVersionCommand(
        UUID workspaceId,
        UUID fileId,
        UUID uploaderUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String expectedCurrentVersion,
        String newVersion,
        String changeMemo) {}
