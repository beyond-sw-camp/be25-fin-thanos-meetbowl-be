package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import com.meetbowl.domain.storage.ObjectStoragePort;
import com.meetbowl.domain.storage.StoredObject;

class DownloadMailAttachmentUseCaseTest {

    @Test
    void senderCanDownloadAttachment() throws IOException {
        UUID senderUserId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        byte[] content = "attachment-content".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        Mail mail = createMail(mailId, senderUserId, recipientUserId, attachmentId);
        StubObjectStorage storage = new StubObjectStorage(new StoredObject(inputStream, "application/pdf", 18));
        DownloadMailAttachmentUseCase useCase =
                new DownloadMailAttachmentUseCase(new StubMailRepository(mail), storage);

        MailAttachmentDownloadResult result =
                useCase.execute(mailId, attachmentId, senderUserId);

        assertEquals("minutes.pdf", result.originalFileName());
        assertEquals("application/pdf", result.contentType());
        assertEquals(18L, result.sizeBytes());
        assertSame(inputStream, result.content());
        assertArrayEquals(content, result.content().readAllBytes());
        assertEquals("mail/object-key", storage.lastDownloadedKey);
    }

    @Test
    void recipientCanDownloadAttachment() {
        UUID senderUserId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Mail mail = createMail(mailId, senderUserId, recipientUserId, attachmentId);
        DownloadMailAttachmentUseCase useCase =
                new DownloadMailAttachmentUseCase(
                        new StubMailRepository(mail),
                        new StubObjectStorage(
                                new StoredObject(
                                        new ByteArrayInputStream(new byte[] {1, 2, 3}),
                                        "application/pdf",
                                        3)));

        MailAttachmentDownloadResult result =
                useCase.execute(mailId, attachmentId, recipientUserId);

        assertEquals("minutes.pdf", result.originalFileName());
    }

    @Test
    void outsiderCannotDownloadAttachment() {
        UUID senderUserId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Mail mail = createMail(mailId, senderUserId, recipientUserId, attachmentId);
        DownloadMailAttachmentUseCase useCase =
                new DownloadMailAttachmentUseCase(
                        new StubMailRepository(mail),
                        new StubObjectStorage(
                                new StoredObject(
                                        new ByteArrayInputStream(new byte[] {1}),
                                        "application/pdf",
                                        1)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(mailId, attachmentId, UUID.randomUUID()));

        assertEquals(ErrorCode.MAIL_FORBIDDEN_ACCESS, exception.errorCode());
    }

    @Test
    void missingAttachmentIsRejected() {
        UUID senderUserId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        Mail mail = createMail(mailId, senderUserId, recipientUserId, UUID.randomUUID());
        DownloadMailAttachmentUseCase useCase =
                new DownloadMailAttachmentUseCase(
                        new StubMailRepository(mail),
                        new StubObjectStorage(
                                new StoredObject(
                                        new ByteArrayInputStream(new byte[] {1}),
                                        "application/pdf",
                                        1)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(mailId, UUID.randomUUID(), senderUserId));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    private static Mail createMail(
            UUID mailId, UUID senderUserId, UUID recipientUserId, UUID attachmentId) {
        return Mail.of(
                mailId,
                UUID.randomUUID(),
                senderUserId,
                List.of(recipientUserId),
                List.of(),
                "첨부 공유",
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
                List.of(
                        MailAttachment.of(
                                attachmentId,
                                senderUserId,
                                "mail/object-key",
                                "minutes.pdf",
                                "stored.pdf",
                                "application/pdf",
                                18L)));
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

    private static final class StubObjectStorage implements ObjectStoragePort {

        private final StoredObject storedObject;
        private String lastDownloadedKey;

        private StubObjectStorage(StoredObject storedObject) {
            this.storedObject = storedObject;
        }

        @Override
        public void upload(String storageKey, String contentType, byte[] content) {}

        @Override
        public StoredObject download(String storageKey) {
            this.lastDownloadedKey = storageKey;
            return storedObject;
        }
    }
}
