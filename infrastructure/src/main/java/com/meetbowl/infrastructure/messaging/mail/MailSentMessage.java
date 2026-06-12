package com.meetbowl.infrastructure.messaging.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.domain.mail.MailSentEvent;

/** 루트 event-contract의 mail.sent payload를 표현하는 RabbitMQ Message DTO다. */
public record MailSentMessage(
        UUID mailId, UUID organizationId, List<UUID> recipientUserIds, Instant sentAt) {

    // 도메인 이벤트를 외부 계약 필드로만 옮긴다. 도메인 모델을 메시지 계약으로 직접 노출하지 않기 위한 경계다.
    static MailSentMessage from(MailSentEvent event) {
        return new MailSentMessage(
                event.mailId(), event.organizationId(), event.recipientUserIds(), event.sentAt());
    }
}
