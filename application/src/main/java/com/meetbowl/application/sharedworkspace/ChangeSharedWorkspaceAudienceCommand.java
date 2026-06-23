package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/**
 * 공유 워크스페이스 공개 범위 변경 입력이다. openToOrganization이 true면 전 직원 공개로, false면 멤버 전용으로 전환한다.
 * requesterIsAdmin은 소유자가 아니어도 관리자 정책으로 공개 범위를 조정할 수 있게 하기 위한 권한 판정 입력이다.
 */
public record ChangeSharedWorkspaceAudienceCommand(
        UUID workspaceId,
        UUID requesterUserId,
        boolean requesterIsAdmin,
        boolean openToOrganization) {}
