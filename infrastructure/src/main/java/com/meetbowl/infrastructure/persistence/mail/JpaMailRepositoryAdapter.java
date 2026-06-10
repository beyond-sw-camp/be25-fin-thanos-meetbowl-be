package com.meetbowl.infrastructure.persistence.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;

@Repository
public class JpaMailRepositoryAdapter implements MailRepositoryPort {

    private final SpringDataMailRepository repository;

    public JpaMailRepositoryAdapter(SpringDataMailRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Mail save(Mail mail) {
        return repository.save(MailEntity.from(mail)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Mail> findById(UUID mailId) {
        return repository.findById(mailId).map(MailEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Mail> findByIdempotencyKey(UUID idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(MailEntity::toDomain);
    }
}
