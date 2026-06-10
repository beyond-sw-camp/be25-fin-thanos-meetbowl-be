package com.meetbowl.infrastructure.persistence.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMailRepository extends JpaRepository<MailEntity, UUID> {

    Optional<MailEntity> findByIdempotencyKey(UUID idempotencyKey);
}
