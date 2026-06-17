package com.meetbowl.infrastructure.messaging.document;

import java.time.Instant;
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
        Metadata metadata,
        AccessScope accessScope) {

    static DocumentIndexRequestedMessage from(DocumentIndexRequestedEvent event) {
        return new DocumentIndexRequestedMessage(
                event.documentId(),
                event.documentType(),
                event.organizationId(),
                event.ownerUserId(),
                event.title(),
                event.content(),
                Metadata.from(event.metadata()),
                new AccessScope(
                        event.userIds(), event.departmentIds(), event.sharedWorkspaceIds()));
    }

    /**
     * sourceType만으로는 부족한 문서별 추가 식별자와 시점을 전달한다. 파일 문서는 본문 대신 storageKey/contentType을 담아 AI가 S3에서 받아
     * 추출하도록 한다.
     */
    public record Metadata(
            UUID meetingId,
            Instant approvedAt,
            UUID workspaceId,
            UUID fileVersionId,
            UUID mailId,
            String storageKey,
            String contentType) {

        static Metadata from(DocumentIndexRequestedEvent.Metadata metadata) {
            if (metadata == null) {
                return new Metadata(null, null, null, null, null, null, null);
            }
            return new Metadata(
                    metadata.meetingId(),
                    metadata.approvedAt(),
                    metadata.workspaceId(),
                    metadata.fileVersionId(),
                    metadata.mailId(),
                    metadata.storageKey(),
                    metadata.contentType());
        }
    }

    /** AI 서버가 Qdrant 검색 필터로 저장해야 하는 문서 열람 범위다. */
    public record AccessScope(
            List<UUID> userIds, List<UUID> departmentIds, List<UUID> sharedWorkspaceIds) {}
}
