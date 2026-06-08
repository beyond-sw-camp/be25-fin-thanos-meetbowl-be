package com.meetbowl.domain.mail;

import java.util.Optional;
import java.util.UUID;

public interface MailRepositoryPort {

    Mail save(Mail mail);

    Optional<Mail> findById(UUID mailId);
}
