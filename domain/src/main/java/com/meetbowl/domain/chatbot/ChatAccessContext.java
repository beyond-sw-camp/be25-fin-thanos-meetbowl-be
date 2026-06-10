package com.meetbowl.domain.chatbot;

import java.util.Set;
import java.util.UUID;

/**
 * 인증과 업무 권한 검증을 마친 뒤 AI 서버에 전달할 검색 허용 범위다.
 *
 * <p>모델이 사용자나 워크스페이스 식별자를 Tool 인자로 만들어 권한을 넓힐 수 없도록, 허용 범위는 Agent 입력과 분리된 신뢰 경계로 유지한다. 빈 범위를 전체
 * 검색으로 해석하지 않도록 자료 유형은 최소 한 개를 요구한다.
 */
public record ChatAccessContext(
        Set<ChatSourceType> allowedSourceTypes, Set<UUID> sharedWorkspaceIds) {

    public ChatAccessContext {
        allowedSourceTypes = allowedSourceTypes == null ? Set.of() : allowedSourceTypes;
        sharedWorkspaceIds = sharedWorkspaceIds == null ? Set.of() : sharedWorkspaceIds;

        if (allowedSourceTypes.isEmpty()) {
            throw ChatDomainValidators.invalid("챗봇 검색에 허용된 자료 유형이 필요합니다.");
        }
        if (allowedSourceTypes.stream().anyMatch(sourceType -> sourceType == null)) {
            throw ChatDomainValidators.invalid("챗봇 검색 자료 유형에 빈 값을 포함할 수 없습니다.");
        }
        if (sharedWorkspaceIds.stream().anyMatch(workspaceId -> workspaceId == null)) {
            throw ChatDomainValidators.invalid("공유 워크스페이스 허용 범위에 빈 ID를 포함할 수 없습니다.");
        }

        allowedSourceTypes = Set.copyOf(allowedSourceTypes);
        sharedWorkspaceIds = Set.copyOf(sharedWorkspaceIds);
    }
}
