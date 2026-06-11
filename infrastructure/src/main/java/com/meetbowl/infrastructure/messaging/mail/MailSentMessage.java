package com.meetbowl.infrastructure.messaging.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.domain.mail.MailSentEvent;

/** 루트 event-contract의 mail.sent payload를 표현하는 RabbitMQ Message DTO다. */
public record MailSentMessage(
        UUID mailId, UUID organizationId, List<UUID> recipientUserIds, Instant sentAt) {

    static MailSentMessage from(MailSentEvent event) {
        return new MailSentMessage(
                event.mailId(), event.organizationId(), event.recipientUserIds(), event.sentAt());
    }
}
