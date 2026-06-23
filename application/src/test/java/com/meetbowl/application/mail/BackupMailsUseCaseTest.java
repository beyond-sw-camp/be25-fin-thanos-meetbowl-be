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

import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

class BackupMailsUseCaseTest {

    @Test
    void backsUpOnlyMailOwnedByCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID backupId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-11T00:00:00Z");
        MailRepositoryPort mailRepository = mock(MailRepositoryPort.class);
        MailboxEntryRepositoryPort mailboxRepository = mock(MailboxEntryRepositoryPort.class);
        PersonalWorkspaceBackupRepositoryPort backupRepository =
                mock(PersonalWorkspaceBackupRepositoryPort.class);
        Mail mail = mock(Mail.class);

        when(mail.subject()).thenReturn("백업 제목");
        when(mail.body()).thenReturn("백업 본문");
        when(mailRepository.findById(mailId)).thenReturn(Optional.of(mail));
        when(mailboxRepository.findAccessibleByMailIdAndOwnerUserId(mailId, userId))
                .thenReturn(Optional.of(mock(MailboxEntry.class)));
        when(backupRepository.findByOwnerUserId(userId)).thenReturn(List.of());
        when(backupRepository.save(any(PersonalWorkspaceBackup.class)))
                .thenAnswer(
                        invocation -> {
                            PersonalWorkspaceBackup backup = invocation.getArgument(0);
                            return PersonalWorkspaceBackup.of(
                                    backupId,
                                    backup.ownerUserId(),
                                    backup.sourceType(),
                                    backup.sourceId(),
                                    backup.title(),
                                    backup.summary(),
                                    backup.backedUpAt());
                        });

        BackupMailsUseCase useCase =
                new BackupMailsUseCase(
                        mailRepository,
                        mailboxRepository,
                        backupRepository,
                        mock(DocumentIndexRequestedEventPort.class),
                        Clock.fixed(now, ZoneOffset.UTC));

        List<BackupMailResult> results =
                useCase.execute(userId, UUID.randomUUID(), List.of(mailId, mailId));

        assertEquals(1, results.size());
        assertEquals(backupId, results.get(0).backupId());
        assertEquals(mailId, results.get(0).mailId());
        assertEquals(now, results.get(0).backedUpAt());
    }
}
