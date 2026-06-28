package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

class SendMailUseCaseTest {

    @Test
    void sendCreatesMailAndMailboxEntriesInSentState() {
        MailRepositoryPort mailRepository = mock(MailRepositoryPort.class);
        MailboxEntryRepositoryPort mailboxRepository = mock(MailboxEntryRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        com.meetbowl.domain.storage.ObjectStoragePort objectStorage =
                mock(com.meetbowl.domain.storage.ObjectStoragePort.class);
        Instant now = Instant.parse("2099-01-01T00:00:00Z");
        UUID organizationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        User sender = mock(User.class);
        User recipient = mock(User.class);

        when(sender.affiliateId()).thenReturn(organizationId);
        when(sender.canLoginAt(now)).thenReturn(true);
        when(sender.isSystemAccount()).thenReturn(false);
        when(recipient.affiliateId()).thenReturn(organizationId);
        when(recipient.canLoginAt(now)).thenReturn(true);
        when(recipient.isSystemAccount()).thenReturn(false);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(mailRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(mailRepository.save(any(Mail.class)))
                .thenAnswer(
                        invocation -> {
                            Mail mail = invocation.getArgument(0);
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
                        });
        when(mailboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMailUseCase useCase =
                new SendMailUseCase(
                        mailRepository,
                        mailboxRepository,
                        userRepository,
                        objectStorage,
                        Clock.fixed(now, ZoneOffset.UTC));

        SendMailResult result =
                useCase.execute(
                        new SendMailCommand(
                                organizationId,
                                senderId,
                                List.of(recipientId),
                                "제목",
                                "본문",
                                "TEXT",
                                null,
                                null,
                                idempotencyKey));

        assertEquals(mailId, result.mailId());
        assertEquals("SENT", result.deliveryStatus());
        assertEquals(now, result.requestedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MailboxEntry>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(mailboxRepository).saveAll(captor.capture());
        assertEquals(
                List.of(MailboxType.SENT, MailboxType.INBOX),
                captor.getValue().stream().map(MailboxEntry::mailboxType).toList());
    }

    @Test
    void sendToSelfCreatesBothSentAndInboxEntriesForSender() {
        MailRepositoryPort mailRepository = mock(MailRepositoryPort.class);
        MailboxEntryRepositoryPort mailboxRepository = mock(MailboxEntryRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        com.meetbowl.domain.storage.ObjectStoragePort objectStorage =
                mock(com.meetbowl.domain.storage.ObjectStoragePort.class);
        Instant now = Instant.parse("2099-01-01T00:00:00Z");
        UUID organizationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        User sender = mock(User.class);

        when(sender.affiliateId()).thenReturn(organizationId);
        when(sender.canLoginAt(now)).thenReturn(true);
        when(sender.isSystemAccount()).thenReturn(false);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(mailRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(mailRepository.save(any(Mail.class)))
                .thenAnswer(
                        invocation -> {
                            Mail mail = invocation.getArgument(0);
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
                        });
        when(mailboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMailUseCase useCase =
                new SendMailUseCase(
                        mailRepository,
                        mailboxRepository,
                        userRepository,
                        objectStorage,
                        Clock.fixed(now, ZoneOffset.UTC));

        useCase.execute(
                new SendMailCommand(
                        organizationId,
                        senderId,
                        List.of(senderId),
                        "내게 보내는 메일",
                        "본문",
                        "TEXT",
                        null,
                        null,
                        idempotencyKey));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MailboxEntry>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(mailboxRepository).saveAll(captor.capture());
        assertEquals(
                List.of(MailboxType.SENT, MailboxType.INBOX),
                captor.getValue().stream().map(MailboxEntry::mailboxType).toList());
        assertEquals(
                List.of(senderId, senderId),
                captor.getValue().stream().map(MailboxEntry::ownerUserId).toList());
    }
}
