package com.meetbowl.infrastructure.persistence.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.domain.mail.MailRetentionPolicyRepositoryPort;

@Repository
public class JpaMailRetentionPolicyRepositoryAdapter implements MailRetentionPolicyRepositoryPort {
    private final SpringDataMailRetentionPolicyRepository repository;

    public JpaMailRetentionPolicyRepositoryAdapter(
            SpringDataMailRetentionPolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    public MailRetentionPolicy save(MailRetentionPolicy mailRetentionPolicy) {
        return repository.save(MailRetentionPolicyEntity.from(mailRetentionPolicy)).toDomain();
    }

    @Override
    public Optional<MailRetentionPolicy> findById(UUID policyId) {
        return repository.findById(policyId).map(MailRetentionPolicyEntity::toDomain);
    }
}
