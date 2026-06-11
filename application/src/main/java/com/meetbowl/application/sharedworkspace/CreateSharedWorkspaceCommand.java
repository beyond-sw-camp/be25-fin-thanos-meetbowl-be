package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/** 공유 워크스페이스 생성 입력이다. 조직과 소유자는 인증 사용자 context에서 채워 넣고 Request DTO를 그대로 전달하지 않는다. */
public record CreateSharedWorkspaceCommand(
        UUID organizationId, UUID ownerUserId, String name, String description) {}
