package com.meetbowl.infrastructure.messaging.mail;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.domain.mail.MailFailedEvent;
import com.meetbowl.domain.mail.MailSentEvent;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** 내부 메일 발송 결과 Adapter가 도메인 이벤트를 mail.sent/mail.failed 계약 Message DTO로 변환하는지 검증한다. */
class RabbitMailDeliveryEventPublisherTest {

    private final RabbitEventPublisher commonPublisher = mock(RabbitEventPublisher.class);
    private final RabbitMailDeliveryEventPublisher publisher =
            new RabbitMailDeliveryEventPublisher(commonPublisher);

    @Test
    void publishSentMapsToMailSentMessage() {
        UUID mailId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Instant sentAt = Instant.parse("2099-01-01T00:00:00Z");

        publisher.publishSent(
                new MailSentEvent(mailId, organizationId, List.of(recipientId), sentAt));

        verify(commonPublisher)
                .publish(
                        eq(EventTypes.MAIL_SENT),
                        argThat(
                                payload -> {
                                    MailSentMessage message = (MailSentMessage) payload;
                                    return message.mailId().equals(mailId)
                                            && message.organizationId().equals(organizationId)
                                            && message.recipientUserIds()
                                                    .equals(List.of(recipientId))
                                            && message.sentAt().equals(sentAt);
                                }));
    }

    @Test
    void publishFailedMapsToMailFailedMessage() {
        UUID mailId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Instant failedAt = Instant.parse("2099-01-01T00:00:00Z");

        publisher.publishFailed(
                new MailFailedEvent(
                        mailId,
                        organizationId,
                        List.of(recipientId),
                        "MAIL_SEND_FAILED",
                        "내부 메일 발송에 실패했습니다.",
                        failedAt,
                        true));

        verify(commonPublisher)
                .publish(
                        eq(EventTypes.MAIL_FAILED),
                        argThat(
                                payload -> {
                                    MailFailedMessage message = (MailFailedMessage) payload;
                                    return message.mailId().equals(mailId)
                                            && message.failureCode().equals("MAIL_SEND_FAILED")
                                            && message.retryable();
                                }));
    }
}
