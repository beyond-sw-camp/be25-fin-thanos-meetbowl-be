package com.meetbowl.domain.mail;

import java.util.Optional;
import java.util.UUID;

public interface MailRetentionPolicyRepositoryPort {

    MailRetentionPolicy save(MailRetentionPolicy mailRetentionPolicy);

    Optional<MailRetentionPolicy> findById(UUID policyId);
}
