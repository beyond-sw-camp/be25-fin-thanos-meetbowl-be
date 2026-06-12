package com.meetbowl.api.sharedworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceMemberResult;

/** 공유 워크스페이스 멤버 응답 DTO다. */
public record SharedWorkspaceMemberResponse(
        UUID workspaceId,
        UUID userId,
        String role,
        String status,
        UUID invitedByUserId,
        Instant joinedAt) {

    public static SharedWorkspaceMemberResponse from(SharedWorkspaceMemberResult result) {
        return new SharedWorkspaceMemberResponse(
                result.workspaceId(),
                result.userId(),
                result.role(),
                result.status(),
                result.invitedByUserId(),
                result.joinedAt());
    }
}
