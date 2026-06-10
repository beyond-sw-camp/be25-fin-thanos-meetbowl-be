package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/** 버전 변경 메모 수정 입력이다. 원본 파일과 저장 경로는 건드리지 않고 해당 버전의 설명 메모만 바꾼다. */
public record UpdateSharedWorkspaceFileVersionMemoCommand(
        UUID workspaceId, UUID fileId, UUID versionId, UUID requesterUserId, String changeMemo) {}
