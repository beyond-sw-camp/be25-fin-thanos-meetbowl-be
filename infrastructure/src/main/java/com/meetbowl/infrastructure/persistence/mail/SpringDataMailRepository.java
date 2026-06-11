package com.meetbowl.infrastructure.persistence.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 메일 엔티티의 Spring Data JPA 리포지토리다. 멱등성 키 조회로 동일 발송 요청의 중복 처리를 막는다. */
interface SpringDataMailRepository extends JpaRepository<MailEntity, UUID> {

    Optional<MailEntity> findByIdempotencyKey(UUID idempotencyKey);
}
