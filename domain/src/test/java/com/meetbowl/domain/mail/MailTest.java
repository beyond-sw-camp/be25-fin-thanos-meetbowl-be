package com.meetbowl.domain.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class MailTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-08T01:00:00Z");

    @Test
    void createDraftKeepsRecipientsAndWaitsForDeliveryRequest() {
        UUID recipientOne = UUID.randomUUID();
        UUID recipientTwo = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        Mail mail =
                createDraft(UUID.randomUUID(), List.of(recipientOne, recipientTwo), idempotencyKey);

        assertEquals(MailDeliveryStatus.DRAFT, mail.deliveryStatus());
        assertEquals(idempotencyKey, mail.idempotencyKey());
        assertEquals(List.of(recipientOne, recipientTwo), mail.recipientUserIds());
        assertNull(mail.requestedAt());
    }

    @Test
    void createDraftFailsWhenRecipientIsDuplicated() {
        UUID recipientUserId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                createDraft(
                                        UUID.randomUUID(),
                                        List.of(recipientUserId, recipientUserId),
                                        UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void bodyCannotExceedDomainLimit() {
        assertThrows(
                BusinessException.class,
                () ->
                        Mail.createDraft(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                List.of(UUID.randomUUID()),
                                List.of(),
                                "제목",
                                "a".repeat(Mail.MAX_BODY_LENGTH + 1),
                                MailType.NORMAL,
                                MailBodyType.TEXT,
                                null,
                                null,
                                UUID.randomUUID()));
    }

    @Test
    void attachmentIsFinalizedBeforeDeliveryRequest() {
        UUID senderUserId = UUID.randomUUID();
        Mail mail = createDraft(senderUserId, List.of(UUID.randomUUID()), UUID.randomUUID());
        mail.addAttachment(createAttachment(senderUserId, "attachments/mail/first.pdf"));

        mail.requestDelivery(REQUESTED_AT);

        assertEquals(MailDeliveryStatus.REQUESTED, mail.deliveryStatus());
        assertEquals(1, mail.attachments().size());
        assertThrows(
                BusinessException.class,
                () ->
                        mail.addAttachment(
                                createAttachment(senderUserId, "attachments/mail/second.pdf")));
    }

    @Test
    void markSentChangesRequestedMailToSent() {
        Mail mail = createRequestedMail();
        Instant sentAt = Instant.parse("2026-06-08T01:01:00Z");

        mail.markSent(sentAt);

        assertEquals(MailDeliveryStatus.SENT, mail.deliveryStatus());
        assertEquals(sentAt, mail.sentAt());
        assertNull(mail.failedAt());
    }

    @Test
    void failedMailCanBeRetriedAndCompleted() {
        Mail mail = createRequestedMail();
        Instant failedAt = Instant.parse("2026-06-08T01:01:00Z");
        Instant retriedAt = Instant.parse("2026-06-08T01:02:00Z");

        mail.markFailed(failedAt, "TEMPORARY_FAILURE");
        mail.retryDelivery(retriedAt);

        assertEquals(MailDeliveryStatus.RETRYING, mail.deliveryStatus());
        assertEquals(1, mail.retryCount());
        assertEquals(retriedAt, mail.requestedAt());
        assertNull(mail.failedAt());
        assertNull(mail.failureCode());

        mail.markSent(Instant.parse("2026-06-08T01:03:00Z"));
        assertEquals(MailDeliveryStatus.SENT, mail.deliveryStatus());
    }

    @Test
    void retryCannotPrecedeFailureTime() {
        Mail mail = createRequestedMail();
        mail.markFailed(Instant.parse("2026-06-08T01:02:00Z"), "FAILED");

        assertThrows(
                BusinessException.class,
                () -> mail.retryDelivery(Instant.parse("2026-06-08T01:01:59Z")));
        assertEquals(MailDeliveryStatus.FAILED, mail.deliveryStatus());
    }

    @Test
    void restoreRejectsInvalidDeliveryState() {
        assertThrows(
                BusinessException.class,
                () ->
                        Mail.of(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                List.of(UUID.randomUUID()),
                                List.of(),
                                "제목",
                                "본문",
                                MailType.NORMAL,
                                MailBodyType.TEXT,
                                null,
                                null,
                                UUID.randomUUID(),
                                MailDeliveryStatus.SENT,
                                REQUESTED_AT,
                                null,
                                null,
                                null,
                                0,
                                List.of()));
    }

    @Test
    void restoreRejectsAttachmentUploadedByNonSender() {
        assertThrows(
                BusinessException.class,
                () ->
                        Mail.of(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                List.of(UUID.randomUUID()),
                                List.of(),
                                "제목",
                                "본문",
                                MailType.NORMAL,
                                MailBodyType.TEXT,
                                null,
                                null,
                                UUID.randomUUID(),
                                MailDeliveryStatus.DRAFT,
                                null,
                                null,
                                null,
                                null,
                                0,
                                List.of(
                                        createAttachment(
                                                UUID.randomUUID(), "attachments/mail/file.pdf"))));
    }

    @Test
    void addAttachmentRejectsDuplicatedObjectKey() {
        UUID senderUserId = UUID.randomUUID();
        Mail mail = createDraft(senderUserId, List.of(UUID.randomUUID()), UUID.randomUUID());
        mail.addAttachment(createAttachment(senderUserId, "attachments/mail/file.pdf"));

        assertThrows(
                BusinessException.class,
                () ->
                        mail.addAttachment(
                                createAttachment(senderUserId, "attachments/mail/file.pdf")));
    }

    private static Mail createRequestedMail() {
        Mail mail = createDraft(UUID.randomUUID(), List.of(UUID.randomUUID()), UUID.randomUUID());
        mail.requestDelivery(REQUESTED_AT);
        return mail;
    }

    private static Mail createDraft(
            UUID senderUserId, List<UUID> recipientUserIds, UUID idempotencyKey) {
        return Mail.createDraft(
                UUID.randomUUID(),
                senderUserId,
                recipientUserIds,
                List.of(),
                "제목",
                "본문",
                MailType.NORMAL,
                MailBodyType.TEXT,
                null,
                null,
                idempotencyKey);
    }

    private static MailAttachment createAttachment(UUID uploaderUserId, String objectKey) {
        return MailAttachment.create(
                uploaderUserId, objectKey, "회의록.pdf", "file.pdf", "application/pdf", 1024);
    }
}
