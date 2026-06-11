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
public class PermanentlyDeleteMailUseCase {

    private final MailboxEntryRepositoryPort repository;
    private final Clock clock;

    @Autowired
    public PermanentlyDeleteMailUseCase(MailboxEntryRepositoryPort repository) {
        this(repository, Clock.systemUTC());
    }

    PermanentlyDeleteMailUseCase(MailboxEntryRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID mailId, UUID ownerUserId) {
        MailboxEntry entry = MailUseCaseSupport.findOwnedEntry(repository, mailId, ownerUserId);
        entry.permanentlyDelete(Instant.now(clock));
        repository.save(entry);
    }
}
