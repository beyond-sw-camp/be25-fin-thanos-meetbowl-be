package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

@Service
public class ChangeMailReadStatusUseCase {

    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final Clock clock;

    @Autowired
    public ChangeMailReadStatusUseCase(MailboxEntryRepositoryPort mailboxEntryRepositoryPort) {
        this(mailboxEntryRepositoryPort, Clock.systemUTC());
    }

    ChangeMailReadStatusUseCase(
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort, Clock clock) {
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID mailId, UUID ownerUserId, boolean read) {
        MailboxEntry entry =
                MailUseCaseSupport.findOwnedEntry(mailboxEntryRepositoryPort, mailId, ownerUserId);
        if (read) {
            entry.markRead(Instant.now(clock));
        } else {
            entry.markUnread();
        }
        mailboxEntryRepositoryPort.save(entry);
    }
}
