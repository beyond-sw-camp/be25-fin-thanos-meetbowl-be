package com.meetbowl.infrastructure.messaging.document;

import java.util.List;
import java.util.UUID;

import com.meetbowl.domain.document.DocumentIndexRequestedEvent;

/** 루트 event-contract의 document.index.requested payload를 표현하는 RabbitMQ Message DTO다. */
public record DocumentIndexRequestedMessage(
        UUID documentId,
        String documentType,
        UUID organizationId,
        UUID ownerUserId,
        String title,
        String content,
        String storageKey,
        String contentType,
        AccessScope accessScope) {

    static DocumentIndexRequestedMessage from(DocumentIndexRequestedEvent event) {
        return new DocumentIndexRequestedMessage(
                event.documentId(),
                event.documentType(),
                event.organizationId(),
                event.ownerUserId(),
                event.title(),
                event.content(),
                event.storageKey(),
                event.contentType(),
                new AccessScope(
                        event.userIds(), event.departmentIds(), event.sharedWorkspaceIds()));
    }

    /** AI 서버가 Qdrant 검색 필터로 저장해야 하는 문서 열람 범위다. */
    public record AccessScope(
            List<UUID> userIds, List<UUID> departmentIds, List<UUID> sharedWorkspaceIds) {}
}
