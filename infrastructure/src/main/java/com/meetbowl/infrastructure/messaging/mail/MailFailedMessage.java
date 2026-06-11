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

    // 도메인 이벤트를 외부 계약 필드로만 옮긴다. 도메인 모델을 메시지 계약으로 직접 노출하지 않기 위한 경계다.
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
