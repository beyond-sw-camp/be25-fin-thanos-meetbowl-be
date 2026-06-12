package com.meetbowl.domain.mail;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 하나의 메일에 대한 특정 사용자의 메일함 상태를 관리한다.
 *
 * <p>읽음, 휴지통 이동, 영구 삭제는 메일 본문이나 다른 수신자의 상태와 독립적으로 변경되어야 한다. 이 상태를 {@link Mail}에서 분리함으로써 한 사용자의 메일함
 * 조작이 공용 메일 데이터와 다른 사용자의 화면에 영향을 주지 않도록 한다.
 */
public class MailboxEntry {

    private final UUID id;
    private final UUID mailId;
    private final UUID ownerUserId;
    private final MailboxType mailboxType;
    private Instant readAt;
    private Instant trashedAt;
    private Instant permanentlyDeletedAt;

    private MailboxEntry(
            UUID id,
            UUID mailId,
            UUID ownerUserId,
            MailboxType mailboxType,
            Instant readAt,
            Instant trashedAt,
            Instant permanentlyDeletedAt) {
        this.id = id;
        this.mailId = requireNonNull(mailId, "메일 ID는 필수입니다.");
        this.ownerUserId = requireNonNull(ownerUserId, "메일함 소유자 ID는 필수입니다.");
        this.mailboxType = requireNonNull(mailboxType, "메일함 유형은 필수입니다.");
        if (mailboxType == MailboxType.SENT && readAt != null) {
            throw invalid("보낸 메일함 항목에는 읽음 시각을 지정할 수 없습니다.");
        }
        validateDeletionState(trashedAt, permanentlyDeletedAt);
        this.readAt = readAt;
        this.trashedAt = trashedAt;
        this.permanentlyDeletedAt = permanentlyDeletedAt;
    }

    public static MailboxEntry inbox(UUID mailId, UUID ownerUserId) {
        return new MailboxEntry(null, mailId, ownerUserId, MailboxType.INBOX, null, null, null);
    }

    public static MailboxEntry sent(UUID mailId, UUID ownerUserId) {
        return new MailboxEntry(null, mailId, ownerUserId, MailboxType.SENT, null, null, null);
    }

    public static MailboxEntry of(
            UUID id,
            UUID mailId,
            UUID ownerUserId,
            MailboxType mailboxType,
            Instant readAt,
            Instant trashedAt,
            Instant permanentlyDeletedAt) {
        return new MailboxEntry(
                id, mailId, ownerUserId, mailboxType, readAt, trashedAt, permanentlyDeletedAt);
    }

    public void markRead(Instant readAt) {
        ensureInbox();
        ensureNotPermanentlyDeleted();
        this.readAt = requireNonNull(readAt, "읽은 시각은 필수입니다.");
    }

    public void markUnread() {
        ensureInbox();
        ensureNotPermanentlyDeleted();
        readAt = null;
    }

    public void moveToTrash(Instant trashedAt) {
        ensureNotPermanentlyDeleted();
        this.trashedAt = requireNonNull(trashedAt, "휴지통 이동 시각은 필수입니다.");
    }

    public void restore() {
        ensureNotPermanentlyDeleted();
        trashedAt = null;
    }

    public void permanentlyDelete(Instant deletedAt) {
        ensureNotPermanentlyDeleted();
        if (trashedAt == null) {
            throw invalid("휴지통에 있는 메일만 영구 삭제할 수 있습니다.");
        }
        Instant validatedDeletedAt = requireNonNull(deletedAt, "영구 삭제 시각은 필수입니다.");
        if (validatedDeletedAt.isBefore(trashedAt)) {
            throw invalid("영구 삭제 시각은 휴지통 이동 시각보다 빠를 수 없습니다.");
        }
        permanentlyDeletedAt = validatedDeletedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public UUID mailId() {
        return mailId;
    }

    public MailboxType mailboxType() {
        return mailboxType;
    }

    public Instant readAt() {
        return readAt;
    }

    public Instant trashedAt() {
        return trashedAt;
    }

    public Instant permanentlyDeletedAt() {
        return permanentlyDeletedAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean isTrashed() {
        return trashedAt != null;
    }

    public boolean isPermanentlyDeleted() {
        return permanentlyDeletedAt != null;
    }

    private void ensureNotPermanentlyDeleted() {
        if (isPermanentlyDeleted()) {
            throw invalid("영구 삭제된 메일함 항목은 변경할 수 없습니다.");
        }
    }

    private void ensureInbox() {
        if (mailboxType != MailboxType.INBOX) {
            throw invalid("받은 메일함 항목만 읽음 상태를 변경할 수 있습니다.");
        }
    }

    private static void validateDeletionState(Instant trashedAt, Instant permanentlyDeletedAt) {
        if (permanentlyDeletedAt != null && trashedAt == null) {
            throw invalid("영구 삭제된 메일함 항목에는 휴지통 이동 시각이 필요합니다.");
        }
        if (permanentlyDeletedAt != null && permanentlyDeletedAt.isBefore(trashedAt)) {
            throw invalid("영구 삭제 시각은 휴지통 이동 시각보다 빠를 수 없습니다.");
        }
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw invalid(message);
        }
        return value;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }
}
