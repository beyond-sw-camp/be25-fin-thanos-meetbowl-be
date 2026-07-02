package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationType;

class ChangeMailReadStatusUseCaseTest {

    @Test
    void readRequestMarksInboxEntryAsReadAndSavesIt() {
        Instant now = Instant.parse("2026-06-25T00:00:00Z");
        UUID mailId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        MailboxEntry entry = MailboxEntry.inbox(mailId, ownerUserId);
        StubMailboxEntryRepository repository = new StubMailboxEntryRepository(entry);
        ChangeMailReadStatusUseCase useCase =
                new ChangeMailReadStatusUseCase(
                        repository,
                        new StubNotificationRepository(),
                        Clock.fixed(now, ZoneOffset.UTC));

        useCase.execute(mailId, ownerUserId, true);

        assertEquals(now, entry.readAt());
        assertSame(entry, repository.savedEntry);
    }

    @Test
    void unreadRequestClearsReadTimestampAndSavesIt() {
        Instant readAt = Instant.parse("2026-06-24T23:59:00Z");
        UUID mailId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        MailboxEntry entry =
                MailboxEntry.of(
                        null, mailId, ownerUserId, MailboxType.INBOX, readAt, null, null);
        StubMailboxEntryRepository repository = new StubMailboxEntryRepository(entry);
        ChangeMailReadStatusUseCase useCase =
                new ChangeMailReadStatusUseCase(
                        repository, new StubNotificationRepository(), Clock.systemUTC());

        useCase.execute(mailId, ownerUserId, false);

        assertFalse(entry.isRead());
        assertSame(entry, repository.savedEntry);
    }

    @Test
    void changingReadStatusOfInaccessibleMailIsRejected() {
        ChangeMailReadStatusUseCase useCase =
                new ChangeMailReadStatusUseCase(
                        new StubMailboxEntryRepository(null),
                        new StubNotificationRepository(),
                        Clock.systemUTC());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID(), true));

        assertEquals(ErrorCode.MAIL_FORBIDDEN_ACCESS, exception.errorCode());
    }

    private static final class StubMailboxEntryRepository implements MailboxEntryRepositoryPort {

        private final MailboxEntry accessibleEntry;
        private MailboxEntry savedEntry;

        private StubMailboxEntryRepository(MailboxEntry accessibleEntry) {
            this.accessibleEntry = accessibleEntry;
        }

        @Override
        public MailboxEntry save(MailboxEntry mailboxEntry) {
            this.savedEntry = mailboxEntry;
            return mailboxEntry;
        }

        @Override
        public List<MailboxEntry> saveAll(List<MailboxEntry> mailboxEntries) {
            return mailboxEntries;
        }

        @Override
        public Optional<MailboxEntry> findAccessibleByMailIdAndOwnerUserId(
                UUID mailId, UUID ownerUserId) {
            return Optional.ofNullable(accessibleEntry)
                    .filter(entry -> entry.mailId().equals(mailId))
                    .filter(entry -> entry.ownerUserId().equals(ownerUserId));
        }

        @Override
        public List<MailboxEntry> findPageByOwnerUserIdAndMailboxType(
                UUID ownerUserId, MailboxType mailboxType, int offset, int limit) {
            return List.of();
        }

        @Override
        public long countByOwnerUserIdAndMailboxType(UUID ownerUserId, MailboxType mailboxType) {
            return 0;
        }

        @Override
        public List<MailboxEntry> findTrashPageByOwnerUserId(UUID ownerUserId, int offset, int limit) {
            return List.of();
        }

        @Override
        public long countTrashByOwnerUserId(UUID ownerUserId) {
            return 0;
        }

        @Override
        public List<MailboxEntry> searchPageByOwnerUserId(
                UUID ownerUserId, String keyword, int offset, int limit) {
            return List.of();
        }

        @Override
        public long countSearchByOwnerUserId(UUID ownerUserId, String keyword) {
            return 0;
        }

        @Override
        public List<MailboxEntry> findActiveEntriesCreatedBefore(
                MailboxType mailboxType, Instant cutoff, int limit) {
            return List.of();
        }

        @Override
        public List<MailboxEntry> findTrashEntriesTrashedBefore(Instant cutoff, int limit) {
            return List.of();
        }
    }

    private static final class StubNotificationRepository implements NotificationRepositoryPort {

        @Override
        public Notification save(Notification notification) {
            return notification;
        }

        @Override
        public Optional<Notification> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Notification> findByRecipientUserId(UUID recipientUserId) {
            return List.of();
        }

        @Override
        public List<Notification> findUnreadByRecipientUserId(UUID recipientUserId) {
            return List.of();
        }

        @Override
        public List<Notification> findPageByRecipientUserId(
                UUID recipientUserId, int offset, int limit) {
            return List.of();
        }

        @Override
        public long countByRecipientUserId(UUID recipientUserId) {
            return 0;
        }

        @Override
        public long countUnreadByRecipientUserId(UUID recipientUserId) {
            return 0;
        }

        @Override
        public List<Notification> findByType(NotificationType type) {
            return List.of();
        }

        @Override
        public Optional<Notification> findLatestByRecipientUserIdAndTypeAndResourceId(
                UUID recipientUserId, NotificationType type, UUID resourceId) {
            return Optional.empty();
        }
    }
}
