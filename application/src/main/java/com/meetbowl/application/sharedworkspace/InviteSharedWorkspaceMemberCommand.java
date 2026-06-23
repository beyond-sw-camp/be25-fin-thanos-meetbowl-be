package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/** 공유 워크스페이스 멤버 초대 입력이다. inviterUserId는 소유자 권한 검증과 초대 이력 기록에 사용한다. */
public record InviteSharedWorkspaceMemberCommand(
        UUID workspaceId, UUID inviterUserId, UUID inviteeUserId) {}
