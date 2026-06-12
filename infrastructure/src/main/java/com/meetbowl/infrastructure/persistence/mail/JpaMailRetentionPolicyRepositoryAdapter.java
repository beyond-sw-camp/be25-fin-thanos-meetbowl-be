package com.meetbowl.infrastructure.persistence.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.domain.mail.MailRetentionPolicyRepositoryPort;

/**
 * 메일 보관 정책의 {@link MailRetentionPolicyRepositoryPort}를 JPA로 구현한다.
 *
 * <p>받은/보낸/휴지통 메일의 보관 기간과 자동 삭제 기준을 저장·조회한다. 도메인↔엔티티 변환은 {@link MailRetentionPolicyEntity}가 맡고 여기서는
 * 위임만 한다.
 */
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
