package com.meetbowl.application.mail;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/**
 * 메일 상세를 조회한다.
 *
 * <p>메일 본문은 공용이지만 열람은 현재 사용자가 소유한 메일함 항목이 있을 때만 허용한다. 본문(Mail)과 사용자별 상태(MailboxEntry)를 함께 묶어 읽음·휴지통
 * 여부까지 한 번에 돌려준다.
 */
@Service
public class GetMailDetailUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;

    public GetMailDetailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
    }

    @Transactional(readOnly = true)
    public MailDetailResult execute(UUID mailId, UUID ownerUserId) {
        Mail mail = MailUseCaseSupport.findMail(mailRepositoryPort, mailId);
        MailboxEntry entry =
                MailUseCaseSupport.findOwnedEntry(mailboxEntryRepositoryPort, mailId, ownerUserId);
        return MailUseCaseSupport.detail(mail, entry);
    }
}
