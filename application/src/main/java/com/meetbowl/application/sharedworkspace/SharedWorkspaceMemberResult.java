package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;

/** 공유 워크스페이스 멤버 응답 모델이다. role/status는 app-api가 domain enum에 의존하지 않도록 String으로 노출한다. */
public record SharedWorkspaceMemberResult(
        UUID workspaceId,
        UUID userId,
        String role,
        String status,
        UUID invitedByUserId,
        Instant joinedAt) {

    public static SharedWorkspaceMemberResult from(SharedWorkspaceMember member) {
        return new SharedWorkspaceMemberResult(
                member.workspaceId(),
                member.userId(),
                member.role().name(),
                member.status().name(),
                member.invitedByUserId(),
                member.joinedAt());
    }
}
