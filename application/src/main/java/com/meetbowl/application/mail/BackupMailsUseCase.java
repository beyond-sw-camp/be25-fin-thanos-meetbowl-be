package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupAttachment;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupSourceType;

/** 사용자가 소유한 메일함 항목을 개인 워크스페이스 백업 자료로 복사한다. */
@Service
public class BackupMailsUseCase {

    private static final int MAX_SUMMARY_LENGTH = 1000;
    private static final String BACKUP_MAIL_DOCUMENT_TYPE = "BACKUP_MAIL";

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final PersonalWorkspaceBackupRepositoryPort backupRepositoryPort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;
    private final Clock clock;

    public BackupMailsUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            PersonalWorkspaceBackupRepositoryPort backupRepositoryPort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort,
            Clock clock) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.backupRepositoryPort = backupRepositoryPort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
        this.clock = clock;
    }

    @Transactional
    public List<BackupMailResult> execute(UUID userId, UUID organizationId, List<UUID> mailIds) {
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
                .map(mailId -> backup(userId, organizationId, mailId, existingMailIds, backedUpAt))
                .toList();
    }

    private BackupMailResult backup(
            UUID userId,
            UUID organizationId,
            UUID mailId,
            Set<UUID> existingMailIds,
            Instant backedUpAt) {
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
                                    mail.body(),
                                    toBackupAttachments(mail),
                                    backedUpAt));
            existingMailIds.add(mailId);
            // 새로 만든 백업만 AI 검색용으로 색인한다(이미 백업된 메일은 내용이 동일하므로 재색인 불필요).
            publishIndexEvent(backup, organizationId);
        }
        return BackupMailResult.from(backup);
    }

    private void publishIndexEvent(PersonalWorkspaceBackup backup, UUID organizationId) {
        // 본문이 비어 있으면 임베딩할 텍스트가 없으므로 색인하지 않는다(AI는 content를 필수로 요구).
        if (backup.body() == null || backup.body().isBlank()) {
            return;
        }
        // 백업메일은 본인 소유 자료이므로 접근 범위를 소유자로 제한한다. organization은 선택값(미소속도 발행).
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        backup.id(),
                        BACKUP_MAIL_DOCUMENT_TYPE,
                        organizationId,
                        backup.ownerUserId(),
                        backup.title(),
                        backup.body(),
                        // 백업 출처인 원본 메일을 metadata.mailId로 연결한다(검색 결과에서 원본 추적용).
                        new DocumentIndexRequestedEvent.Metadata(
                                null, null, null, null, backup.sourceId(), null, null),
                        List.of(backup.ownerUserId()),
                        List.of(),
                        List.of()));
    }

    /**
     * 원본 메일이 보존 정책으로 삭제돼도 백업이 자기완결적이도록 첨부 메타데이터를 스냅샷한다.
     *
     * <p>현재는 원본 첨부의 object key를 그대로 복사해 보관한다. 보존 정책이 첨부 파일 실체(S3 객체)까지 삭제하므로, 백업 시점에 S3 객체를 백업 전용
     * 키로 복사하고 그 키를 저장하는 단계는 스토리지 어댑터 연동(후속 작업)에서 적용한다.
     */
    private List<PersonalWorkspaceBackupAttachment> toBackupAttachments(Mail mail) {
        return mail.attachments().stream()
                .map(
                        attachment ->
                                PersonalWorkspaceBackupAttachment.of(
                                        attachment.objectKey(),
                                        attachment.originalFileName(),
                                        attachment.storedFileName(),
                                        attachment.mimeType(),
                                        attachment.sizeBytes()))
                .toList();
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
