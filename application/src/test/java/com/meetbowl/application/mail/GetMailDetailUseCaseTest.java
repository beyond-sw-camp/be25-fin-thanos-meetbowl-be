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
import com.meetbowl.domain.mail.MailAttachment;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

class GetMailDetailUseCaseTest {

    @Test
    void detailContainsMailBodyMailboxStateAndAttachments() {
        UUID ownerUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        Mail mail =
                Mail.of(
                        mailId,
                        UUID.randomUUID(),
                        senderUserId,
                        List.of(ownerUserId),
                        List.of(),
                        "회의 결과 공유",
                        "본문입니다.",
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
                        List.of(
                                MailAttachment.of(
                                        attachmentId,
                                        senderUserId,
                                        "mail/attachment-1",
                                        "minutes.pdf",
                                        "stored.pdf",
                                        "application/pdf",
                                        42L)));
        MailboxEntry entry =
                MailboxEntry.of(
                        null,
                        mailId,
                        ownerUserId,
                        MailboxType.INBOX,
                        Instant.parse("2026-06-24T00:10:00Z"),
                        null,
                        null);
        GetMailDetailUseCase useCase =
                new GetMailDetailUseCase(
                        new StubMailRepository(mail), new StubMailboxEntryRepository(entry));

        MailDetailResult result = useCase.execute(mailId, ownerUserId);

        assertEquals(mailId, result.mailId());
        assertEquals("회의 결과 공유", result.subject());
        assertEquals("INBOX", result.mailboxType());
        assertEquals(1, result.attachments().size());
        assertEquals(attachmentId, result.attachments().get(0).attachmentId());
    }

    @Test
    void detailFailsWhenMailDoesNotExist() {
        GetMailDetailUseCase useCase =
                new GetMailDetailUseCase(
                        new StubMailRepository(null), new StubMailboxEntryRepository(null));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID()));

        assertEquals(ErrorCode.MAIL_NOT_FOUND, exception.errorCode());
    }

    @Test
    void detailFailsWhenMailboxEntryIsNotOwnedByRequester() {
        UUID mailId = UUID.randomUUID();
        Mail mail = createMail(mailId, UUID.randomUUID());
        GetMailDetailUseCase useCase =
                new GetMailDetailUseCase(
                        new StubMailRepository(mail), new StubMailboxEntryRepository(null));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(mailId, UUID.randomUUID()));

        assertEquals(ErrorCode.MAIL_FORBIDDEN_ACCESS, exception.errorCode());
    }

    private static Mail createMail(UUID mailId, UUID recipientId) {
        return Mail.of(
                mailId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(recipientId),
                List.of(),
                "제목",
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

    private record StubMailRepository(Mail mail) implements MailRepositoryPort {

        @Override
        public Mail save(Mail mail) {
            return mail;
        }

        @Override
        public Optional<Mail> findById(UUID mailId) {
            return Optional.ofNullable(mail).filter(value -> value.id().equals(mailId));
        }

        @Override
        public Optional<Mail> findByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }
    }

    private record StubMailboxEntryRepository(MailboxEntry entry)
            implements MailboxEntryRepositoryPort {

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
            return Optional.ofNullable(entry)
                    .filter(value -> value.mailId().equals(mailId))
                    .filter(value -> value.ownerUserId().equals(ownerUserId));
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
