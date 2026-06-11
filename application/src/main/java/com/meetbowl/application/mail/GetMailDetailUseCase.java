package com.meetbowl.application.mail;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

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
