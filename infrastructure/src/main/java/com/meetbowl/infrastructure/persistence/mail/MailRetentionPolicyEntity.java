package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 메일 보존 정책을 저장하는 엔티티다. 편지함 유형별 보존 일수와 자동 삭제 여부/갱신 정보를 관리한다. */
@Entity
@Table(name = "mail_retention_policies")
public class MailRetentionPolicyEntity extends BaseEntity {
    /** 받은편지함 보존일(일 단위). */
    @Column(nullable = false)
    private int inboxRetentionDays;

    /** 보낸편지함 보존일(일 단위). */
    @Column(nullable = false)
    private int sentRetentionDays;

    /** 휴지통 보존일(일 단위). */
    @Column(nullable = false)
    private int trashRetentionDays;

    /** 보존 기간 초과 메일 자동 삭제 여부. */
    @Column(nullable = false)
    private boolean autoDeleteEnabled;

    /** 정책을 마지막으로 갱신한 관리자/사용자 ID(UUID). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    /** 정책 갱신 시각(UTC). */
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
