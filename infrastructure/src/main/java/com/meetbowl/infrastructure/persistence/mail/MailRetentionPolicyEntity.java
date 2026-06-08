package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "mail_retention_policies")
public class MailRetentionPolicyEntity extends BaseEntity {
    @Column(nullable = false)
    private int inboxRetentionDays;

    @Column(nullable = false)
    private int sentRetentionDays;

    @Column(nullable = false)
    private int trashRetentionDays;

    @Column(nullable = false)
    private boolean autoDeleteEnabled;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    @Column(nullable = false)
    private Instant policyUpdatedAt;

    protected MailRetentionPolicyEntity() {}

    private MailRetentionPolicyEntity(MailRetentionPolicy policy) {
        inboxRetentionDays = policy.inboxRetentionDays();
        sentRetentionDays = policy.sentRetentionDays();
        trashRetentionDays = policy.trashRetentionDays();
        autoDeleteEnabled = policy.autoDeleteEnabled();
        updatedBy = policy.updatedBy();
        policyUpdatedAt = policy.updatedAt();
    }

    static MailRetentionPolicyEntity from(MailRetentionPolicy policy) {
        MailRetentionPolicyEntity entity = new MailRetentionPolicyEntity(policy);
        entity.setId(policy.id());
        return entity;
    }

    MailRetentionPolicy toDomain() {
        return new MailRetentionPolicy(
                getId(),
                inboxRetentionDays,
                sentRetentionDays,
                trashRetentionDays,
                autoDeleteEnabled,
                updatedBy,
                policyUpdatedAt);
    }
}
