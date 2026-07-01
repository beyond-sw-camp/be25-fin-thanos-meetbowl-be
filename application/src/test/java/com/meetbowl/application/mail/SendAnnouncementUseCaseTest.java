package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

class SendAnnouncementUseCaseTest {

    private final MailRepositoryPort mailRepository = mock(MailRepositoryPort.class);
    private final MailboxEntryRepositoryPort mailboxRepository =
            mock(MailboxEntryRepositoryPort.class);
    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final MailNotificationService mailNotificationService = mock(MailNotificationService.class);
    private final Instant now = Instant.parse("2099-01-01T00:00:00Z");
    private final SendAnnouncementUseCase useCase =
            new SendAnnouncementUseCase(
                mailRepository,
                mailboxRepository,
                userRepository,
                mailNotificationService,
                Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void announcementGoesToActiveOrgMembersExceptSenderAndSystem() {
        UUID organizationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID activeRecipientId = UUID.randomUUID();
        UUID inactiveRecipientId = UUID.randomUUID();
        UUID systemAccountId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        User sender = user(senderId, organizationId, true, false);
        User activeRecipient = user(activeRecipientId, organizationId, true, false);
        User inactiveRecipient = user(inactiveRecipientId, organizationId, false, false);
        User systemAccount = user(systemAccountId, organizationId, true, true);

        when(mailRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(userRepository.findAllByAffiliateId(organizationId))
                .thenReturn(List.of(sender, activeRecipient, inactiveRecipient, systemAccount));
        when(mailRepository.save(any(Mail.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), mailId));
        when(mailboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMailResult result =
                useCase.execute(
                        new SendAnnouncementCommand(
                                organizationId,
                                senderId,
                                "공지",
                                "전 직원 공지입니다.",
                                "TEXT",
                                idempotencyKey));

        assertEquals(mailId, result.mailId());
        assertEquals("SENT", result.deliveryStatus());

        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        assertEquals(MailType.ANNOUNCEMENT, mailCaptor.getValue().mailType());
        assertEquals(List.of(activeRecipientId), mailCaptor.getValue().recipientUserIds());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MailboxEntry>> entryCaptor = ArgumentCaptor.forClass(List.class);
        verify(mailboxRepository).saveAll(entryCaptor.capture());
        assertEquals(
                List.of(MailboxType.SENT, MailboxType.INBOX),
                entryCaptor.getValue().stream().map(MailboxEntry::mailboxType).toList());
        ArgumentCaptor<Mail> notificationMailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailNotificationService).notifyRecipients(notificationMailCaptor.capture());
        assertEquals(mailId, notificationMailCaptor.getValue().id());
    }

    @Test
    void announcementWithoutEligibleRecipientsIsRejected() {
        UUID organizationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        User onlySender = user(senderId, organizationId, true, false);
        when(mailRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(userRepository.findAllByAffiliateId(organizationId)).thenReturn(List.of(onlySender));

        assertThrows(
                BusinessException.class,
                () ->
                        useCase.execute(
                                new SendAnnouncementCommand(
                                        organizationId,
                                        senderId,
                                        "공지",
                                        "본문",
                                        "TEXT",
                                        UUID.randomUUID())));
    }

    private User user(UUID id, UUID organizationId, boolean canLogin, boolean system) {
        User user = mock(User.class);
        when(user.id()).thenReturn(id);
        when(user.affiliateId()).thenReturn(organizationId);
        when(user.canLoginAt(now)).thenReturn(canLogin);
        when(user.isSystemAccount()).thenReturn(system);
        return user;
    }

    private Mail withId(Mail mail, UUID mailId) {
        return Mail.of(
                mailId,
                mail.organizationId(),
                mail.senderUserId(),
                mail.recipientUserIds(),
                mail.externalRecipients(),
                mail.subject(),
                mail.body(),
                mail.mailType(),
                mail.bodyType(),
                mail.relatedResourceType(),
                mail.relatedResourceId(),
                mail.idempotencyKey(),
                mail.deliveryStatus(),
                mail.requestedAt(),
                mail.sentAt(),
                mail.failedAt(),
                mail.failureCode(),
                mail.retryCount(),
                mail.attachments());
    }
}
