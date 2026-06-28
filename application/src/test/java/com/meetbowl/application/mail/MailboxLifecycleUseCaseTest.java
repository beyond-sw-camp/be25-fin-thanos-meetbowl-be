package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class MailboxLifecycleUseCaseTest {

    @Test
    void moveToTrashStoresTrashTimestamp() {
        Instant now = Instant.parse("2026-06-25T01:00:00Z");
        MailboxEntry entry = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        StubMailboxEntryRepository repository = new StubMailboxEntryRepository(entry);
        MoveMailToTrashUseCase useCase =
                new MoveMailToTrashUseCase(repository, Clock.fixed(now, ZoneOffset.UTC));

        useCase.execute(entry.mailId(), entry.ownerUserId());

        assertEquals(now, entry.trashedAt());
        assertSame(entry, repository.savedEntry);
    }

    @Test
    void moveToTrashRejectsAlreadyTrashedEntry() {
        Instant trashedAt = Instant.parse("2026-06-25T00:30:00Z");
        MailboxEntry entry =
                MailboxEntry.of(
                        null,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MailboxType.INBOX,
                        null,
                        trashedAt,
                        null);
        MoveMailToTrashUseCase useCase =
                new MoveMailToTrashUseCase(
                        new StubMailboxEntryRepository(entry), Clock.systemUTC());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(entry.mailId(), entry.ownerUserId()));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void restoreClearsTrashTimestamp() {
        MailboxEntry entry =
                MailboxEntry.of(
                        null,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MailboxType.INBOX,
                        null,
                        Instant.parse("2026-06-25T00:30:00Z"),
                        null);
        StubMailboxEntryRepository repository = new StubMailboxEntryRepository(entry);
        RestoreMailUseCase useCase = new RestoreMailUseCase(repository);

        useCase.execute(entry.mailId(), entry.ownerUserId());

        assertFalse(entry.isTrashed());
        assertNull(entry.trashedAt());
        assertSame(entry, repository.savedEntry);
    }

    @Test
    void restoreRejectsEntryThatIsNotInTrash() {
        MailboxEntry entry = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        RestoreMailUseCase useCase = new RestoreMailUseCase(new StubMailboxEntryRepository(entry));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(entry.mailId(), entry.ownerUserId()));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void permanentlyDeleteMarksDeletionTimestamp() {
        Instant trashedAt = Instant.parse("2026-06-25T00:30:00Z");
        Instant deletedAt = Instant.parse("2026-06-25T02:00:00Z");
        MailboxEntry entry =
                MailboxEntry.of(
                        null,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MailboxType.INBOX,
                        null,
                        trashedAt,
                        null);
        StubMailboxEntryRepository repository = new StubMailboxEntryRepository(entry);
        PermanentlyDeleteMailUseCase useCase =
                new PermanentlyDeleteMailUseCase(
                        repository, Clock.fixed(deletedAt, ZoneOffset.UTC));

        useCase.execute(entry.mailId(), entry.ownerUserId());

        assertEquals(deletedAt, entry.permanentlyDeletedAt());
        assertSame(entry, repository.savedEntry);
    }

    @Test
    void permanentlyDeleteRejectsEntryOutsideTrash() {
        MailboxEntry entry = MailboxEntry.inbox(UUID.randomUUID(), UUID.randomUUID());
        PermanentlyDeleteMailUseCase useCase =
                new PermanentlyDeleteMailUseCase(
                        new StubMailboxEntryRepository(entry), Clock.systemUTC());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(entry.mailId(), entry.ownerUserId()));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
        assertNotNull(exception.getMessage());
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
}
