package com.meetbowl.domain.document;

import java.util.List;
import java.util.UUID;

/** AI 검색/RAG에 사용할 문서 본문과 접근 범위를 색인 서버에 전달하는 도메인 이벤트다. */
public record DocumentIndexRequestedEvent(
        UUID documentId,
        String documentType,
        UUID organizationId,
        UUID ownerUserId,
        String title,
        String content,
        String storageKey,
        String contentType,
        List<UUID> userIds,
        List<UUID> departmentIds,
        List<UUID> sharedWorkspaceIds) {

    public DocumentIndexRequestedEvent {
        // 발행 요청 이후 호출자가 목록을 변경해 이벤트 계약이 달라지는 것을 방지한다.
        userIds = List.copyOf(userIds);
        departmentIds = List.copyOf(departmentIds);
        sharedWorkspaceIds = List.copyOf(sharedWorkspaceIds);
    }

    /** 회의록·메모·메일처럼 본문을 직접 전달하는 기존 호출의 계약을 유지한다. */
    public DocumentIndexRequestedEvent(
            UUID documentId,
            String documentType,
            UUID organizationId,
            UUID ownerUserId,
            String title,
            String content,
            List<UUID> userIds,
            List<UUID> departmentIds,
            List<UUID> sharedWorkspaceIds) {
        this(
                documentId,
                documentType,
                organizationId,
                ownerUserId,
                title,
                content,
                null,
                null,
                userIds,
                departmentIds,
                sharedWorkspaceIds);
    }
}
