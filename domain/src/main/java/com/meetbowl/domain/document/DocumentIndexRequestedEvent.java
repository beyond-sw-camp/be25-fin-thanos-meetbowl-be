package com.meetbowl.domain.document;

import java.time.Instant;
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
        Metadata metadata,
        List<UUID> userIds,
        List<UUID> departmentIds,
        List<UUID> sharedWorkspaceIds) {

    public DocumentIndexRequestedEvent {
        // 발행 요청 이후 호출자가 목록을 변경해 이벤트 계약이 달라지는 것을 방지한다.
        userIds = List.copyOf(userIds);
        departmentIds = List.copyOf(departmentIds);
        sharedWorkspaceIds = List.copyOf(sharedWorkspaceIds);
    }

    /**
     * 문서 종류별 추가 식별자와 시점을 담는 metadata다.
     *
     * <p>공통 필드만 최상위에 두고 문서 전용 정보는 metadata에 모아야 회의록 외 문서가 같은
     * 색인 파이프라인을 사용해도 필드 의미가 섞이지 않는다.
     */
    public record Metadata(
            UUID meetingId,
            Instant approvedAt,
            UUID workspaceId,
            UUID fileVersionId,
            UUID mailId) {}
}
