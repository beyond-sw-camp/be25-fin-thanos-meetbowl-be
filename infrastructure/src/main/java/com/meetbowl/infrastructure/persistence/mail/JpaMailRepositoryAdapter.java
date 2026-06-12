package com.meetbowl.infrastructure.persistence.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailRepositoryPort;

/**
 * 메일 애그리거트의 {@link MailRepositoryPort}를 JPA로 구현한다.
 *
 * <p>도메인 모델과 JPA 엔티티의 변환은 {@link MailEntity}가 맡고, 이 어댑터는 저장/조회 위임만 한다. 멱등성 키 조회를 노출해 동일 발송 요청을 중복
 * 처리하지 않도록 UseCase가 선행 조회할 수 있게 한다.
 */
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
