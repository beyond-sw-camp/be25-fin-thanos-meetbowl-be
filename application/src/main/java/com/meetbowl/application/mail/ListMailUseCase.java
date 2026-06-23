package com.meetbowl.application.mail;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

/**
 * 받은/보낸/휴지통 메일함 목록을 페이지로 조회한다.
 *
 * <p>메일함 항목(MailboxEntry)을 페이지로 먼저 조회한 뒤 각 항목의 공용 메일 본문(Mail)을 붙여 요약을 만든다. 받은/보낸 목록은 휴지통 항목을 제외하고,
 * 휴지통 목록은 받은·보낸 유형을 함께 모은다(영구 삭제 항목은 모두 제외). page는 1부터 시작한다.
 */
@Service
public class ListMailUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;

    public ListMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
    }

    @Transactional(readOnly = true)
    public MailPageResult inbox(UUID ownerUserId, int page, int size) {
        return mailbox(ownerUserId, MailboxType.INBOX, page, size);
    }

    @Transactional(readOnly = true)
    public MailPageResult sent(UUID ownerUserId, int page, int size) {
        return mailbox(ownerUserId, MailboxType.SENT, page, size);
    }

    @Transactional(readOnly = true)
    public MailPageResult trash(UUID ownerUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<MailboxEntry> entries =
                mailboxEntryRepositoryPort.findTrashPageByOwnerUserId(ownerUserId, offset, size);
        long total = mailboxEntryRepositoryPort.countTrashByOwnerUserId(ownerUserId);
        return page(entries, page, size, total);
    }

    private MailPageResult mailbox(UUID ownerUserId, MailboxType mailboxType, int page, int size) {
        int offset = (page - 1) * size;
        List<MailboxEntry> entries =
                mailboxEntryRepositoryPort.findPageByOwnerUserIdAndMailboxType(
                        ownerUserId, mailboxType, offset, size);
        long total =
                mailboxEntryRepositoryPort.countByOwnerUserIdAndMailboxType(
                        ownerUserId, mailboxType);
        return page(entries, page, size, total);
    }

    private MailPageResult page(
            List<MailboxEntry> entries, int page, int size, long totalElements) {
        List<MailSummaryResult> items =
                entries.stream()
                        .map(entry -> MailUseCaseSupport.summary(findMail(entry), entry))
                        .toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new MailPageResult(items, page, size, totalElements, totalPages);
    }

    private Mail findMail(MailboxEntry entry) {
        return MailUseCaseSupport.findMail(mailRepositoryPort, entry.mailId());
    }
}
