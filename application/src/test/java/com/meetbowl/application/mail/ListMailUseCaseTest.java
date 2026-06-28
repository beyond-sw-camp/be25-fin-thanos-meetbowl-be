package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

class ListMailUseCaseTest {

    @Test
    void inboxReturnsPagedSummaries() {
        UUID ownerUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        StubMailRepository mailRepository =
                new StubMailRepository(List.of(createMail(mailId, ownerUserId, "받은 메일")));
        StubMailboxEntryRepository mailboxRepository =
                new StubMailboxEntryRepository(
                        List.of(MailboxEntry.inbox(mailId, ownerUserId)), 3L, List.of(), 0L);
        ListMailUseCase useCase = new ListMailUseCase(mailRepository, mailboxRepository);

        MailPageResult result = useCase.inbox(ownerUserId, 2, 2);

        assertEquals(1, result.items().size());
        assertEquals("받은 메일", result.items().get(0).subject());
        assertEquals(2, result.page());
        assertEquals(2, result.size());
        assertEquals(3L, result.totalElements());
        assertEquals(2, result.totalPages());
    }

    @Test
    void trashUsesTrashQueryAndCeilingPageCalculation() {
        UUID ownerUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        StubMailRepository mailRepository =
                new StubMailRepository(List.of(createMail(mailId, ownerUserId, "휴지통 메일")));
        MailboxEntry trashedEntry =
                MailboxEntry.of(
                        null,
                        mailId,
                        ownerUserId,
                        MailboxType.INBOX,
                        null,
                        Instant.parse("2026-06-24T10:00:00Z"),
                        null);
        StubMailboxEntryRepository mailboxRepository =
                new StubMailboxEntryRepository(List.of(), 0L, List.of(trashedEntry), 5L);
        ListMailUseCase useCase = new ListMailUseCase(mailRepository, mailboxRepository);

        MailPageResult result = useCase.trash(ownerUserId, 1, 2);

        assertEquals(1, result.items().size());
        assertEquals(true, result.items().get(0).trashed());
        assertEquals(3, result.totalPages());
    }

    @Test
    void listingFailsWhenReferencedMailIsMissing() {
        UUID ownerUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        StubMailboxEntryRepository mailboxRepository =
                new StubMailboxEntryRepository(
                        List.of(MailboxEntry.sent(mailId, ownerUserId)), 1L, List.of(), 0L);
        ListMailUseCase useCase =
                new ListMailUseCase(new StubMailRepository(List.of()), mailboxRepository);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.sent(ownerUserId, 1, 20));

        assertEquals(ErrorCode.MAIL_NOT_FOUND, exception.errorCode());
    }

    private static Mail createMail(UUID mailId, UUID recipientId, String subject) {
        return Mail.of(
                mailId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(recipientId),
                List.of(),
                subject,
                "본문",
                MailType.NORMAL,
                MailBodyType.TEXT,
                null,
                null,
                UUID.randomUUID(),
                MailDeliveryStatus.SENT,
                Instant.parse("2026-06-24T00:00:00Z"),
                Instant.parse("2026-06-24T00:01:00Z"),
                null,
                null,
                0,
                List.of());
    }

    private record StubMailRepository(List<Mail> mails) implements MailRepositoryPort {

        @Override
        public Mail save(Mail mail) {
            return mail;
        }

        @Override
        public Optional<Mail> findById(UUID mailId) {
            return mails.stream().filter(mail -> mail.id().equals(mailId)).findFirst();
        }

        @Override
        public Optional<Mail> findByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }
    }

    private static final class StubMailboxEntryRepository implements MailboxEntryRepositoryPort {

        private final List<MailboxEntry> mailboxEntries;
        private final long mailboxTotal;
        private final List<MailboxEntry> trashEntries;
        private final long trashTotal;

        private StubMailboxEntryRepository(
                List<MailboxEntry> mailboxEntries,
                long mailboxTotal,
                List<MailboxEntry> trashEntries,
                long trashTotal) {
            this.mailboxEntries = mailboxEntries;
            this.mailboxTotal = mailboxTotal;
            this.trashEntries = trashEntries;
            this.trashTotal = trashTotal;
        }

        @Override
        public MailboxEntry save(MailboxEntry mailboxEntry) {
            return mailboxEntry;
        }

        @Override
        public List<MailboxEntry> saveAll(List<MailboxEntry> mailboxEntries) {
            return mailboxEntries;
        }

        @Override
        public Optional<MailboxEntry> findAccessibleByMailIdAndOwnerUserId(
                UUID mailId, UUID ownerUserId) {
            return Optional.empty();
        }

        @Override
        public List<MailboxEntry> findPageByOwnerUserIdAndMailboxType(
                UUID ownerUserId, MailboxType mailboxType, int offset, int limit) {
            return mailboxEntries;
        }

        @Override
        public long countByOwnerUserIdAndMailboxType(UUID ownerUserId, MailboxType mailboxType) {
            return mailboxTotal;
        }

        @Override
        public List<MailboxEntry> findTrashPageByOwnerUserId(UUID ownerUserId, int offset, int limit) {
            return trashEntries;
        }

        @Override
        public long countTrashByOwnerUserId(UUID ownerUserId) {
            return trashTotal;
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
