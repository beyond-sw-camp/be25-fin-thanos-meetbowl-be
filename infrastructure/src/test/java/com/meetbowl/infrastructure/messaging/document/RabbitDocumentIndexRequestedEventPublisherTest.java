package com.meetbowl.infrastructure.messaging.document;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** document.index.requested Adapter가 본문과 접근 범위를 계약 Message DTO로 변환하는지 검증한다. */
class RabbitDocumentIndexRequestedEventPublisherTest {

    @Test
    void publishDocumentIndexRequestedMessage() {
        RabbitEventPublisher commonPublisher = mock(RabbitEventPublisher.class);
        RabbitDocumentIndexRequestedEventPublisher publisher =
                new RabbitDocumentIndexRequestedEventPublisher(commonPublisher);
        UUID minutesId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();

        publisher.publish(
                new DocumentIndexRequestedEvent(
                        minutesId,
                        "MEETING_MINUTES",
                        organizationId,
                        reviewerUserId,
                        "회의록",
                        "색인할 회의 요약",
                        List.of(reviewerUserId),
                        List.of(),
                        List.of()));

        verify(commonPublisher)
                .publish(
                        eq(EventTypes.DOCUMENT_INDEX_REQUESTED),
                        argThat(
                                payload -> {
                                    DocumentIndexRequestedMessage message =
                                            (DocumentIndexRequestedMessage) payload;
                                    return message.documentId().equals(minutesId)
                                            && message.documentType().equals("MEETING_MINUTES")
                                            && message.organizationId().equals(organizationId)
                                            && message.ownerUserId().equals(reviewerUserId)
                                            && message.title().equals("회의록")
                                            && message.content().equals("색인할 회의 요약")
                                            && message.accessScope()
                                                    .userIds()
                                                    .equals(List.of(reviewerUserId))
                                            && message.accessScope().departmentIds().isEmpty()
                                            && message.accessScope().sharedWorkspaceIds().isEmpty();
                                }));
    }

    @Test
    void publishFileDocumentIndexRequestedMessage() {
        RabbitEventPublisher commonPublisher = mock(RabbitEventPublisher.class);
        RabbitDocumentIndexRequestedEventPublisher publisher =
                new RabbitDocumentIndexRequestedEventPublisher(commonPublisher);
        UUID fileId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        String storageKey = "personal-drive/" + ownerUserId + "/" + UUID.randomUUID();

        publisher.publish(
                new DocumentIndexRequestedEvent(
                        fileId,
                        "PERSONAL_DRIVE_FILE",
                        null,
                        ownerUserId,
                        "자료.pdf",
                        null,
                        storageKey,
                        "application/pdf",
                        List.of(ownerUserId),
                        List.of(),
                        List.of()));

        verify(commonPublisher)
                .publish(
                        eq(EventTypes.DOCUMENT_INDEX_REQUESTED),
                        argThat(
                                payload -> {
                                    DocumentIndexRequestedMessage message =
                                            (DocumentIndexRequestedMessage) payload;
                                    return message.documentId().equals(fileId)
                                            && message.content() == null
                                            && message.storageKey().equals(storageKey)
                                            && message.contentType().equals("application/pdf")
                                            && message.accessScope()
                                                    .userIds()
                                                    .equals(List.of(ownerUserId));
                                }));
    }
}
