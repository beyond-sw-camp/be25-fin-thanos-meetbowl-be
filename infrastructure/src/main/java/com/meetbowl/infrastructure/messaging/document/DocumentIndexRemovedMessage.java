package com.meetbowl.infrastructure.messaging.document;

import java.util.UUID;

import com.meetbowl.domain.document.DocumentIndexRemovedEvent;

/** 루트 event-contract의 document.index.removed payload를 표현하는 RabbitMQ Message DTO다. */
public record DocumentIndexRemovedMessage(UUID documentId) {

    static DocumentIndexRemovedMessage from(DocumentIndexRemovedEvent event) {
        return new DocumentIndexRemovedMessage(event.documentId());
    }
}
