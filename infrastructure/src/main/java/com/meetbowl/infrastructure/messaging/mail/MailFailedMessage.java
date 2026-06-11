package com.meetbowl.infrastructure.messaging.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.domain.mail.MailFailedEvent;

/** 루트 event-contract의 mail.failed payload를 표현하는 RabbitMQ Message DTO다. */
public record MailFailedMessage(
        UUID mailId,
        UUID organizationId,
        List<UUID> recipientUserIds,
        String failureCode,
        String failureReason,
        Instant failedAt,
        boolean retryable) {

    static MailFailedMessage from(MailFailedEvent event) {
        return new MailFailedMessage(
                event.mailId(),
                event.organizationId(),
                event.recipientUserIds(),
                event.failureCode(),
                event.failureReason(),
                event.failedAt(),
                event.retryable());
    }
}
