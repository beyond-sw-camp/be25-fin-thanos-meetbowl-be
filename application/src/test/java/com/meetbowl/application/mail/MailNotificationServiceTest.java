package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.RelatedResourceType;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

class MailNotificationServiceTest {

    private final DispatchNotificationUseCase dispatchNotificationUseCase =
            mock(DispatchNotificationUseCase.class);
    private final MailNotificationService service =
            new MailNotificationService(dispatchNotificationUseCase);

    @Test
    void notifiesNormalMailRecipientsAsMailReceived() {
        UUID mailId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Mail mail =
                mail(
                        mailId,
                        List.of(recipientId),
                        "일반 메일",
                        MailBodyType.TEXT,
                        null,
                        null);

        service.notifyRecipients(mail);

        ArgumentCaptor<DispatchNotificationCommand> captor =
                ArgumentCaptor.forClass(DispatchNotificationCommand.class);
        verify(dispatchNotificationUseCase).execute(captor.capture());
        DispatchNotificationCommand command = captor.getValue();
        assertEquals(recipientId, command.recipientUserId());
        assertEquals(NotificationType.MAIL_RECEIVED.name(), command.type());
        assertEquals(NotificationResourceType.MAIL.name(), command.resourceType());
        assertEquals(mailId, command.resourceId());
    }

    @Test
    void notifiesMinutesShareMailRecipientsAsMailShared() {
        UUID mailId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Mail mail =
                mail(
                        mailId,
                        List.of(recipientId),
                        "회의록 공유",
                        MailBodyType.MINUTES_SHARE,
                        RelatedResourceType.MEETING_MINUTES,
                        minutesId);

        service.notifyRecipients(mail);

        ArgumentCaptor<DispatchNotificationCommand> captor =
                ArgumentCaptor.forClass(DispatchNotificationCommand.class);
        verify(dispatchNotificationUseCase).execute(captor.capture());
        DispatchNotificationCommand command = captor.getValue();
        assertEquals(NotificationType.MAIL_SHARED.name(), command.type());
        assertEquals(NotificationResourceType.MAIL.name(), command.resourceType());
        assertEquals(mailId, command.resourceId());
    }

    private Mail mail(
            UUID mailId,
            List<UUID> recipientIds,
            String subject,
            MailBodyType bodyType,
            RelatedResourceType relatedResourceType,
            UUID relatedResourceId) {
        Instant now = Instant.parse("2099-01-01T00:00:00Z");
        return Mail.of(
                mailId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                recipientIds,
                List.of(),
                subject,
                "본문",
                MailType.NORMAL,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                UUID.randomUUID(),
                MailDeliveryStatus.SENT,
                now,
                now,
                null,
                null,
                0,
                List.of());
    }
}
