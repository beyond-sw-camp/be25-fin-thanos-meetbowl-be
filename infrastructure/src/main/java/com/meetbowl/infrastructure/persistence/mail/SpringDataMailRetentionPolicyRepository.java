package com.meetbowl.infrastructure.persistence.mail;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 메일 보관 정책 엔티티의 Spring Data JPA 리포지토리다. */
public interface SpringDataMailRetentionPolicyRepository
        extends JpaRepository<MailRetentionPolicyEntity, UUID> {}
