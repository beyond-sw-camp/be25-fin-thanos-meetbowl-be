package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupSourceType;

/** 사용자가 소유한 메일함 항목을 개인 워크스페이스 백업 자료로 복사한다. */
@Service
public class BackupMailsUseCase {

    private static final int MAX_SUMMARY_LENGTH = 1000;

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final PersonalWorkspaceBackupRepositoryPort backupRepositoryPort;
    private final Clock clock;

    public BackupMailsUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            PersonalWorkspaceBackupRepositoryPort backupRepositoryPort,
            Clock clock) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.backupRepositoryPort = backupRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public List<BackupMailResult> execute(UUID userId, List<UUID> mailIds) {
        Set<UUID> existingMailIds =
                backupRepositoryPort.findByOwnerUserId(userId).stream()
                        .filter(
                                backup ->
                                        backup.sourceType()
                                                == PersonalWorkspaceBackupSourceType.MAIL)
                        .map(PersonalWorkspaceBackup::sourceId)
                        .collect(Collectors.toSet());

        Instant backedUpAt = Instant.now(clock);
        return mailIds.stream()
                .distinct()
                .map(mailId -> backup(userId, mailId, existingMailIds, backedUpAt))
                .toList();
    }

    private BackupMailResult backup(
            UUID userId, UUID mailId, Set<UUID> existingMailIds, Instant backedUpAt) {
        Mail mail = MailUseCaseSupport.findMail(mailRepositoryPort, mailId);
        MailUseCaseSupport.findOwnedEntry(mailboxEntryRepositoryPort, mailId, userId);

        PersonalWorkspaceBackup backup;
        if (existingMailIds.contains(mailId)) {
            backup =
                    backupRepositoryPort.findByOwnerUserId(userId).stream()
                            .filter(existing -> existing.sourceId().equals(mailId))
                            .findFirst()
                            .orElseThrow();
        } else {
            backup =
                    backupRepositoryPort.save(
                            PersonalWorkspaceBackup.create(
                                    userId,
                                    PersonalWorkspaceBackupSourceType.MAIL,
                                    mailId,
                                    mail.subject(),
                                    summarize(mail.body()),
                                    backedUpAt));
            existingMailIds.add(mailId);
        }
        return BackupMailResult.from(backup);
    }

    private String summarize(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String normalized = body.trim();
        return normalized.length() <= MAX_SUMMARY_LENGTH
                ? normalized
                : normalized.substring(0, MAX_SUMMARY_LENGTH);
    }
}
