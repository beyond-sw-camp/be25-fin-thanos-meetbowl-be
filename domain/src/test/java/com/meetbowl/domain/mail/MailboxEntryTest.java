package com.meetbowl.domain.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class MailboxEntryTest {

    private static final UUID MAIL_ID = UUID.randomUUID();

    @Test
    void inboxEntryManagesReadAndTrashState() {
        MailboxEntry entry = MailboxEntry.inbox(MAIL_ID, UUID.randomUUID());
        Instant readAt = Instant.parse("2026-06-08T02:00:00Z");
        Instant trashedAt = Instant.parse("2026-06-08T03:00:00Z");

        entry.markRead(readAt);
        entry.moveToTrash(trashedAt);

        assertTrue(entry.isRead());
        assertEquals(readAt, entry.readAt());
        assertTrue(entry.isTrashed());

        entry.markUnread();
        entry.restore();

        assertFalse(entry.isRead());
        assertFalse(entry.isTrashed());
    }

    @Test
    void permanentDeleteRequiresTrashState() {
        MailboxEntry entry = MailboxEntry.inbox(MAIL_ID, UUID.randomUUID());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> entry.permanentlyDelete(Instant.parse("2026-06-08T04:00:00Z")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void permanentlyDeletedEntryCannotBeChanged() {
        MailboxEntry entry = MailboxEntry.inbox(MAIL_ID, UUID.randomUUID());
        entry.moveToTrash(Instant.parse("2026-06-08T03:00:00Z"));
        entry.permanentlyDelete(Instant.parse("2026-06-08T04:00:00Z"));

        assertTrue(entry.isPermanentlyDeleted());
        assertThrows(BusinessException.class, entry::restore);
        assertThrows(
                BusinessException.class,
                () -> entry.permanentlyDelete(Instant.parse("2026-06-08T05:00:00Z")));
    }

    @Test
    void permanentDeleteRejectsTimeBeforeTrashTime() {
        MailboxEntry entry = MailboxEntry.inbox(MAIL_ID, UUID.randomUUID());
        entry.moveToTrash(Instant.parse("2026-06-08T03:00:00Z"));

        assertThrows(
                BusinessException.class,
                () -> entry.permanentlyDelete(Instant.parse("2026-06-08T02:59:59Z")));
    }

    @Test
    void sentEntryCannotChangeReadState() {
        MailboxEntry entry = MailboxEntry.sent(MAIL_ID, UUID.randomUUID());

        assertThrows(
                BusinessException.class,
                () -> entry.markRead(Instant.parse("2026-06-08T02:00:00Z")));
    }
}
