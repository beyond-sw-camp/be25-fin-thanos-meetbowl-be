package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;

/**
 * 공유 워크스페이스 단건/목록 응답의 application 출력 모델이다. app-api는 domain 패키지를 알 수 없으므로 visibility 같은 도메인 enum을
 * String으로 평탄화해서 노출한다.
 */
public record SharedWorkspaceResult(
        UUID workspaceId,
        UUID organizationId,
        UUID ownerUserId,
        String name,
        String description,
        String visibility,
        Instant createdAt) {

    public static SharedWorkspaceResult from(SharedWorkspace workspace) {
        return new SharedWorkspaceResult(
                workspace.id(),
                workspace.organizationId(),
                workspace.ownerUserId(),
                workspace.name(),
                workspace.description(),
                workspace.visibility().name(),
                workspace.createdAt());
    }
}
