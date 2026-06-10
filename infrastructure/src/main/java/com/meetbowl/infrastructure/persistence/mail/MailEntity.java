package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.RelatedResourceType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "mail",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_mail_idempotency_key",
                        columnNames = "idempotency_key"),
        indexes = {
            @Index(
                    name = "idx_mail_organization_requested_at",
                    columnList = "organization_id, requested_at"),
            @Index(
                    name = "idx_mail_sender_requested_at",
                    columnList = "sender_user_id, requested_at"),
            @Index(name = "idx_mail_delivery_status", columnList = "delivery_status")
        })
public class MailEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(name = "sender_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID senderUserId;

    @Column(nullable = false, length = Mail.MAX_SUBJECT_LENGTH)
    private String subject;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String body;

    @ElementCollection
    @CollectionTable(
            name = "mail_recipient",
            joinColumns = @JoinColumn(name = "mail_id"),
            uniqueConstraints =
                    @UniqueConstraint(
                            name = "uk_mail_recipient_mail_user",
                            columnNames = {"mail_id", "recipient_user_id"}),
            indexes = @Index(name = "idx_mail_recipient_user", columnList = "recipient_user_id"))
    @OrderColumn(name = "recipient_order", nullable = false)
    @Column(name = "recipient_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private List<UUID> recipientUserIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false, length = 30)
    private MailType mailType;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false, length = 30)
    private MailBodyType bodyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_resource_type", length = 40)
    private RelatedResourceType relatedResourceType;

    @Column(name = "related_resource_id", columnDefinition = "BINARY(16)")
    private UUID relatedResourceId;

    @Column(name = "idempotency_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private MailDeliveryStatus deliveryStatus;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MailAttachmentEntity> attachments = new ArrayList<>();

    protected MailEntity() {}

    static MailEntity from(Mail mail) {
        MailEntity entity = new MailEntity();
        entity.setId(mail.id());
        entity.organizationId = mail.organizationId();
        entity.senderUserId = mail.senderUserId();
        entity.recipientUserIds.addAll(mail.recipientUserIds());
        entity.subject = mail.subject();
        entity.body = mail.body();
        entity.mailType = mail.mailType();
        entity.bodyType = mail.bodyType();
        entity.relatedResourceType = mail.relatedResourceType();
        entity.relatedResourceId = mail.relatedResourceId();
        entity.idempotencyKey = mail.idempotencyKey();
        entity.deliveryStatus = mail.deliveryStatus();
        entity.requestedAt = mail.requestedAt();
        entity.sentAt = mail.sentAt();
        entity.failedAt = mail.failedAt();
        entity.failureCode = mail.failureCode();
        entity.retryCount = mail.retryCount();
        mail.attachments()
                .forEach(
                        attachment ->
                                entity.attachments.add(
                                        MailAttachmentEntity.from(entity, attachment)));
        return entity;
    }

    Mail toDomain() {
        return Mail.of(
                getId(),
                organizationId,
                senderUserId,
                recipientUserIds,
                subject,
                body,
                mailType,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                idempotencyKey,
                deliveryStatus,
                requestedAt,
                sentAt,
                failedAt,
                failureCode,
                retryCount,
                attachments.stream().map(MailAttachmentEntity::toDomain).toList());
    }
}
