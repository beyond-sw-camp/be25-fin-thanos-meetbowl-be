package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/**
 * 받은 메일의 읽음/안읽음 상태를 변경한다.
 *
 * <p>읽음 상태는 공용 메일 본문이 아니라 사용자별 메일함 항목(MailboxEntry)에만 기록하므로, 같은 메일을 받은 다른 수신자의 화면에는 영향을 주지 않는다.
 */
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
