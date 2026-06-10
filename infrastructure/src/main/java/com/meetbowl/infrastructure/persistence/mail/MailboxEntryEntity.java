package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * {@link MailboxEntry}의 사용자별 상태를 메일 본문과 분리해 저장하는 infrastructure 전용 엔티티다.
 *
 * <p>동일 메일과 소유자, 메일함 유형의 조합은 하나만 존재해야 화면 상태가 모호해지지 않으므로 DB 유일성 제약으로 애플리케이션 검증의 경쟁 조건을 보완한다. 메일과는
 * ID로만 연결해 사용자 상태 변경이 메일 애그리거트의 JPA 연관관계 변경으로 번지지 않게 한다.
 */
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

    @Column(name = "mail_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID mailId;

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

    static MailboxEntryEntity from(MailboxEntry entry) {
        MailboxEntryEntity entity = new MailboxEntryEntity();
        entity.setId(entry.id());
        entity.mailId = entry.mailId();
        entity.ownerUserId = entry.ownerUserId();
        entity.mailboxType = entry.mailboxType();
        entity.readAt = entry.readAt();
        entity.trashedAt = entry.trashedAt();
        entity.permanentlyDeletedAt = entry.permanentlyDeletedAt();
        return entity;
    }

    MailboxEntry toDomain() {
        return MailboxEntry.of(
                getId(), mailId, ownerUserId, mailboxType, readAt, trashedAt, permanentlyDeletedAt);
    }
}
