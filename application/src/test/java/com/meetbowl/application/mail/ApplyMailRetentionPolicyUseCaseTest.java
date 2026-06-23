package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.domain.mail.MailRetentionPolicyRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

@ExtendWith(MockitoExtension.class)
class ApplyMailRetentionPolicyUseCaseTest {

    private static final UUID POLICY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-06-23T03:00:00Z");

    @Mock private MailRetentionPolicyRepositoryPort policyRepositoryPort;
    @Mock private MailboxEntryRepositoryPort mailboxEntryRepositoryPort;

    @Test
    void executeSkipsWhenPolicyDoesNotExist() {
        ApplyMailRetentionPolicyUseCase useCase = useCase();
        given(policyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.empty());

        MailRetentionApplyResult result = useCase.execute();

        assertEquals(false, result.enabled());
        assertEquals(0, result.inboxMovedToTrashCount());
        assertEquals(0, result.sentMovedToTrashCount());
        assertEquals(0, result.trashPermanentlyDeletedCount());
        verifyNoInteractions(mailboxEntryRepositoryPort);
    }

    @Test
    void executeSkipsWhenAutoDeleteDisabled() {
        ApplyMailRetentionPolicyUseCase useCase = useCase();
        given(policyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.of(policy(false)));

        MailRetentionApplyResult result = useCase.execute();

        assertEquals(false, result.enabled());
        verifyNoInteractions(mailboxEntryRepositoryPort);
    }

    @Test
    void executeMovesExpiredInboxAndSentToTrashAndDeletesExpiredTrash() {
        ApplyMailRetentionPolicyUseCase useCase = useCase();
        MailboxEntry inboxEntry = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        MailboxEntry sentEntry = MailboxEntry.sent(UUID.randomUUID(), UUID.randomUUID());
        MailboxEntry trashEntry = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        trashEntry.moveToTrash(Instant.parse("2026-06-01T00:00:00Z"));

        given(policyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.of(policy(true)));
        given(
                        mailboxEntryRepositoryPort.findActiveEntriesCreatedBefore(
                                eq(MailboxType.INBOX),
                                eq(Instant.parse("2026-06-16T03:00:00Z")),
                                eq(500)))
                .willReturn(List.of(inboxEntry), List.of());
        given(
                        mailboxEntryRepositoryPort.findActiveEntriesCreatedBefore(
                                eq(MailboxType.SENT),
                                eq(Instant.parse("2026-06-16T03:00:00Z")),
                                eq(500)))
                .willReturn(List.of(sentEntry), List.of());
        given(
                        mailboxEntryRepositoryPort.findTrashEntriesTrashedBefore(
                                eq(Instant.parse("2026-06-16T03:00:00Z")), eq(500)))
                .willReturn(List.of(trashEntry), List.of());

        MailRetentionApplyResult result = useCase.execute();

        assertEquals(true, result.enabled());
        assertEquals(1, result.inboxMovedToTrashCount());
        assertEquals(1, result.sentMovedToTrashCount());
        assertEquals(1, result.trashPermanentlyDeletedCount());
        assertEquals(NOW, inboxEntry.trashedAt());
        assertEquals(NOW, sentEntry.trashedAt());
        assertEquals(NOW, trashEntry.permanentlyDeletedAt());

        ArgumentCaptor<List<MailboxEntry>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(mailboxEntryRepositoryPort, times(3)).saveAll(saveCaptor.capture());
        assertEquals(3, saveCaptor.getAllValues().size());
    }

    @Test
    void executeProcessesExpiredEntriesByBatchSizeWithoutOffsetSkipping() {
        ApplyMailRetentionPolicyUseCase useCase = useCase(2);
        MailboxEntry first = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        MailboxEntry second = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        MailboxEntry third = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());

        given(policyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.of(policy(true)));
        given(
                        mailboxEntryRepositoryPort.findActiveEntriesCreatedBefore(
                                eq(MailboxType.INBOX),
                                eq(Instant.parse("2026-06-16T03:00:00Z")),
                                eq(2)))
                .willReturn(List.of(first, second), List.of(third), List.of());
        given(
                        mailboxEntryRepositoryPort.findActiveEntriesCreatedBefore(
                                eq(MailboxType.SENT),
                                eq(Instant.parse("2026-06-16T03:00:00Z")),
                                eq(2)))
                .willReturn(List.of());
        given(
                        mailboxEntryRepositoryPort.findTrashEntriesTrashedBefore(
                                eq(Instant.parse("2026-06-16T03:00:00Z")), eq(2)))
                .willReturn(List.of());

        MailRetentionApplyResult result = useCase.execute();

        assertEquals(true, result.enabled());
        assertEquals(3, result.inboxMovedToTrashCount());
        assertEquals(0, result.sentMovedToTrashCount());
        assertEquals(0, result.trashPermanentlyDeletedCount());
        assertEquals(NOW, first.trashedAt());
        assertEquals(NOW, second.trashedAt());
        assertEquals(NOW, third.trashedAt());
        verify(mailboxEntryRepositoryPort, times(3))
                .findActiveEntriesCreatedBefore(
                        eq(MailboxType.INBOX), eq(Instant.parse("2026-06-16T03:00:00Z")), eq(2));
        verify(mailboxEntryRepositoryPort, times(2)).saveAll(any());
    }

    @Test
    void executeDoesNotSaveWhenNoExpiredEntriesExist() {
        ApplyMailRetentionPolicyUseCase useCase = useCase();
        given(policyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.of(policy(true)));
        given(mailboxEntryRepositoryPort.findActiveEntriesCreatedBefore(any(), any(), anyInt()))
                .willReturn(List.of());
        given(mailboxEntryRepositoryPort.findTrashEntriesTrashedBefore(any(), anyInt()))
                .willReturn(List.of());

        MailRetentionApplyResult result = useCase.execute();

        assertEquals(true, result.enabled());
        assertNotNull(result);
        verify(mailboxEntryRepositoryPort, never()).saveAll(any());
    }

    private ApplyMailRetentionPolicyUseCase useCase() {
        return useCase(500);
    }

    private ApplyMailRetentionPolicyUseCase useCase(int batchSize) {
        return new ApplyMailRetentionPolicyUseCase(
                policyRepositoryPort,
                mailboxEntryRepositoryPort,
                Clock.fixed(NOW, ZoneOffset.UTC),
                batchSize);
    }

    private MailRetentionPolicy policy(boolean autoDeleteEnabled) {
        return new MailRetentionPolicy(
                POLICY_ID,
                7,
                7,
                7,
                autoDeleteEnabled,
                ADMIN_ID,
                Instant.parse("2026-06-20T00:00:00Z"));
    }
}
