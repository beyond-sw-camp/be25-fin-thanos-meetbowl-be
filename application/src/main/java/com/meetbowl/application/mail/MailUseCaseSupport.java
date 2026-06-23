package com.meetbowl.application.mail;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/**
 * 메일 조회/상태 변경 UseCase들이 공통으로 쓰는 조회·변환 헬퍼다.
 *
 * <p>"메일 본문 조회"와 "내 메일함 항목 조회"를 한곳에 모아, 각 UseCase가 소유권 검증과 결과 변환 규칙을 제각각 구현해 어긋나는 것을 막는다.
 */
final class MailUseCaseSupport {

    private MailUseCaseSupport() {}

    static Mail findMail(MailRepositoryPort repository, UUID mailId) {
        return repository
                .findById(mailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_NOT_FOUND));
    }

    static MailboxEntry findOwnedEntry(
            MailboxEntryRepositoryPort repository, UUID mailId, UUID ownerUserId) {
        // 남의 메일함 항목이거나 없을 때 존재 여부를 흘리지 않도록 NOT_FOUND가 아닌 FORBIDDEN으로 통일한다.
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
                entry.trashedAt(),
                mail.attachments().stream()
                        .map(
                                attachment ->
                                        new MailAttachmentInfo(
                                                attachment.id(),
                                                attachment.originalFileName(),
                                                attachment.mimeType(),
                                                attachment.sizeBytes()))
                        .toList());
    }
}
