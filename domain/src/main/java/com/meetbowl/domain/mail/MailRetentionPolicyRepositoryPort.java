package com.meetbowl.domain.mail;

import java.util.Optional;
import java.util.UUID;

/** 메일 보관 정책 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. */
public interface MailRetentionPolicyRepositoryPort {

    MailRetentionPolicy save(MailRetentionPolicy mailRetentionPolicy);

    Optional<MailRetentionPolicy> findById(UUID policyId);
}
