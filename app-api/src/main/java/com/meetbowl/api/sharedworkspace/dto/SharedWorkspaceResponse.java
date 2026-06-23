package com.meetbowl.api.sharedworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceResult;

/** 공유 워크스페이스 응답 DTO다. app-api는 domain을 알 수 없으므로 application Result만 변환해 외부 계약을 구성한다. */
public record SharedWorkspaceResponse(
        UUID workspaceId,
        UUID organizationId,
        UUID ownerUserId,
        String name,
        String description,
        String visibility,
        Instant createdAt) {

    public static SharedWorkspaceResponse from(SharedWorkspaceResult result) {
        return new SharedWorkspaceResponse(
                result.workspaceId(),
                result.organizationId(),
                result.ownerUserId(),
                result.name(),
                result.description(),
                result.visibility(),
                result.createdAt());
    }
}
