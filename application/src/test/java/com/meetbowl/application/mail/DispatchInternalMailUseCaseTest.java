package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryEventPort;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailFailedEvent;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailSentEvent;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

class DispatchInternalMailUseCaseTest {

    private final Instant now = Instant.parse("2099-01-01T00:00:00Z");
    private final UUID organizationId = UUID.randomUUID();
    private final UUID senderId = UUID.randomUUID();
    private final UUID recipientId = UUID.randomUUID();
    private final UUID idempotencyKey = UUID.randomUUID();

    private final MailRepositoryPort mailRepository = mock(MailRepositoryPort.class);
    private final MailboxEntryRepositoryPort mailboxRepository =
            mock(MailboxEntryRepositoryPort.class);
    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final MailDeliveryEventPort deliveryEventPort = mock(MailDeliveryEventPort.class);

    private final DispatchInternalMailUseCase useCase =
            new DispatchInternalMailUseCase(
                    mailRepository,
                    mailboxRepository,
                    userRepository,
                    deliveryEventPort,
                    Clock.fixed(now, ZoneOffset.UTC));

    private DispatchInternalMailCommand command() {
        return new DispatchInternalMailCommand(
                organizationId,
                senderId,
                List.of(recipientId),
                "회의록 공유",
                "회의록을 공유합니다.",
                "MINUTES_SHARE",
                "MEETING_MINUTES",
                UUID.randomUUID(),
                idempotencyKey);
    }

    private void givenActiveParticipants() {
        User sender = mock(User.class);
        User recipient = mock(User.class);
        when(sender.affiliateId()).thenReturn(organizationId);
        when(recipient.affiliateId()).thenReturn(organizationId);
        when(recipient.canLoginAt(now)).thenReturn(true);
        when(recipient.isSystemAccount()).thenReturn(false);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
    }

    @Test
    void dispatchPersistsSystemMailAndPublishesSentEvent() {
        givenActiveParticipants();
        when(mailRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(mailRepository.save(any(Mail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mailboxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMailResult result = useCase.execute(command());

        assertEquals("SENT", result.deliveryStatus());
        assertEquals(now, result.requestedAt());

        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        Mail persisted = mailCaptor.getValue();
        // 내부 발송은 SYSTEM 유형으로 저장하고, 영속 전에 발급한 ID가 결과/이벤트와 같아야 한다.
        assertEquals(MailType.SYSTEM, persisted.mailType());
        assertEquals(MailDeliveryStatus.SENT, persisted.deliveryStatus());
        assertEquals(result.mailId(), persisted.id());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MailboxEntry>> entryCaptor = ArgumentCaptor.forClass(List.class);
        verify(mailboxRepository).saveAll(entryCaptor.capture());
        assertEquals(
                List.of(MailboxType.SENT, MailboxType.INBOX),
                entryCaptor.getValue().stream().map(MailboxEntry::mailboxType).toList());

        ArgumentCaptor<MailSentEvent> eventCaptor = ArgumentCaptor.forClass(MailSentEvent.class);
        verify(deliveryEventPort).publishSent(eventCaptor.capture());
        assertEquals(result.mailId(), eventCaptor.getValue().mailId());
        assertEquals(List.of(recipientId), eventCaptor.getValue().recipientUserIds());
        verify(deliveryEventPort, never()).publishFailed(any());
    }

    @Test
    void publishesFailedEventWhenPersistenceFails() {
        givenActiveParticipants();
        when(mailRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(mailRepository.save(any(Mail.class)))
                .thenThrow(new RuntimeException("DB unavailable"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.execute(command()));
        assertEquals(ErrorCode.MAIL_SEND_FAILED, exception.errorCode());

        ArgumentCaptor<MailFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(MailFailedEvent.class);
        verify(deliveryEventPort).publishFailed(eventCaptor.capture());
        assertEquals("MAIL_SEND_FAILED", eventCaptor.getValue().failureCode());
        assertEquals(true, eventCaptor.getValue().retryable());
        verify(deliveryEventPort, never()).publishSent(any());
    }

    @Test
    void returnsExistingResultWithoutRepublishingWhenIdempotentRequestRepeats() {
        UUID mailId = UUID.randomUUID();
        UUID relatedResourceId = UUID.randomUUID();
        DispatchInternalMailCommand command =
                new DispatchInternalMailCommand(
                        organizationId,
                        senderId,
                        List.of(recipientId),
                        "회의록 공유",
                        "회의록을 공유합니다.",
                        "MINUTES_SHARE",
                        "MEETING_MINUTES",
                        relatedResourceId,
                        idempotencyKey);
        Mail existing =
                Mail.of(
                        mailId,
                        organizationId,
                        senderId,
                        List.of(recipientId),
                        "회의록 공유",
                        "회의록을 공유합니다.",
                        MailType.SYSTEM,
                        MailBodyType.MINUTES_SHARE,
                        com.meetbowl.domain.mail.RelatedResourceType.MEETING_MINUTES,
                        relatedResourceId,
                        idempotencyKey,
                        MailDeliveryStatus.SENT,
                        now,
                        now,
                        null,
                        null,
                        0,
                        List.of());
        when(mailRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        SendMailResult result = useCase.execute(command);

        assertEquals(mailId, result.mailId());
        verify(mailRepository, never()).save(any());
        verify(deliveryEventPort, never()).publishSent(any());
        verify(deliveryEventPort, never()).publishFailed(any());
    }
}
