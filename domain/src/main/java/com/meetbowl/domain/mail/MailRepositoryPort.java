package com.meetbowl.domain.mail;

import java.util.Optional;
import java.util.UUID;

/** 메일 도메인이 저장 기술을 알지 않고 애그리거트 단위로 영속화를 요청하기 위한 경계다. */
public interface MailRepositoryPort {

    Mail save(Mail mail);

    Optional<Mail> findById(UUID mailId);

    Optional<Mail> findByIdempotencyKey(UUID idempotencyKey);
}
