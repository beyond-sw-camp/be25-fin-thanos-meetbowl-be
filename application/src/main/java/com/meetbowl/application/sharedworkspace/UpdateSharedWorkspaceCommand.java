package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/** 공유 워크스페이스 이름·설명 수정 입력이다. 요청자는 소유자 권한 검증에 사용한다. */
public record UpdateSharedWorkspaceCommand(
        UUID workspaceId, UUID requesterUserId, String name, String description) {}
