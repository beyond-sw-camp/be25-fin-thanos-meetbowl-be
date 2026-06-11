package com.meetbowl.application.mail;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

final class MailUseCaseSupport {

    private MailUseCaseSupport() {}

    static Mail findMail(MailRepositoryPort repository, UUID mailId) {
        return repository
                .findById(mailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_NOT_FOUND));
    }

    static MailboxEntry findOwnedEntry(
            MailboxEntryRepositoryPort repository, UUID mailId, UUID ownerUserId) {
        return repository
                .findAccessibleByMailIdAndOwnerUserId(mailId, ownerUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_FORBIDDEN_ACCESS));
    }

    static MailSummaryResult summary(Mail mail, MailboxEntry entry) {
        return new MailSummaryResult(
                mail.id(),
                mail.senderUserId(),
                mail.recipientUserIds(),
                mail.subject(),
                mail.mailType().name(),
                mail.deliveryStatus().name(),
                entry.mailboxType().name(),
                entry.isRead(),
                entry.isTrashed(),
                mail.requestedAt());
    }

    static MailDetailResult detail(Mail mail, MailboxEntry entry) {
        return new MailDetailResult(
                mail.id(),
                mail.organizationId(),
                mail.senderUserId(),
                mail.recipientUserIds(),
                mail.subject(),
                mail.body(),
                mail.mailType().name(),
                mail.bodyType().name(),
                mail.relatedResourceType() == null ? null : mail.relatedResourceType().name(),
                mail.relatedResourceId(),
                mail.deliveryStatus().name(),
                entry.mailboxType().name(),
                mail.requestedAt(),
                mail.sentAt(),
                entry.isRead(),
                entry.readAt(),
                entry.isTrashed(),
                entry.trashedAt());
    }
}
