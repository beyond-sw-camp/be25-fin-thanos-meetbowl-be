package com.meetbowl.domain.mail;

import java.util.Optional;
import java.util.UUID;

public interface MailboxEntryRepositoryPort {

    MailboxEntry save(MailboxEntry mailboxEntry);

    Optional<MailboxEntry> findByMailIdAndOwnerUserId(UUID mailId, UUID ownerUserId);
}
