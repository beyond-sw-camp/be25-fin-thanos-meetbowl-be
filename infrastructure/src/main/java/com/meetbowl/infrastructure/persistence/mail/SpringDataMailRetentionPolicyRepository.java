package com.meetbowl.infrastructure.persistence.mail;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMailRetentionPolicyRepository
        extends JpaRepository<MailRetentionPolicyEntity, UUID> {}
