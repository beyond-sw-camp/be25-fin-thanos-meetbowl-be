package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "mailbox_entry",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_mailbox_entry_mail_owner_type",
                        columnNames = {"mail_id", "owner_user_id", "mailbox_type"}),
        indexes = {
            @Index(
                    name = "idx_mailbox_entry_owner_type",
                    columnList = "owner_user_id, mailbox_type"),
            @Index(
                    name = "idx_mailbox_entry_owner_trashed",
                    columnList = "owner_user_id, trashed_at")
        })
public class MailboxEntryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mail_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_mailbox_entry_mail"))
    private MailEntity mail;

    @Column(name = "owner_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mailbox_type", nullable = false, length = 20)
    private MailboxType mailboxType;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "trashed_at")
    private Instant trashedAt;

    @Column(name = "permanently_deleted_at")
    private Instant permanentlyDeletedAt;

    protected MailboxEntryEntity() {}

    static MailboxEntryEntity from(MailEntity mail, MailboxEntry entry) {
        MailboxEntryEntity entity = new MailboxEntryEntity();
        entity.setId(entry.id());
        entity.mail = mail;
        entity.ownerUserId = entry.ownerUserId();
        entity.mailboxType = entry.mailboxType();
        entity.readAt = entry.readAt();
        entity.trashedAt = entry.trashedAt();
        entity.permanentlyDeletedAt = entry.permanentlyDeletedAt();
        return entity;
    }

    MailboxEntry toDomain() {
        return MailboxEntry.of(
                getId(), ownerUserId, mailboxType, readAt, trashedAt, permanentlyDeletedAt);
    }
}
