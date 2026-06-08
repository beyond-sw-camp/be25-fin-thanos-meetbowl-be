package com.meetbowl.domain.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class MailTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-06-08T01:00:00Z");

    @Test
    void requestMailCreatesSenderAndRecipientMailboxEntries() {
        UUID senderUserId = UUID.randomUUID();
        UUID recipientOne = UUID.randomUUID();
        UUID recipientTwo = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        Mail mail =
                Mail.request(
                        UUID.randomUUID(),
                        senderUserId,
                        List.of(recipientOne, recipientTwo),
                        "회의록 공유",
                        "승인된 회의록을 공유합니다.",
                        MailType.NORMAL,
                        MailBodyType.MINUTES_SHARE,
                        RelatedResourceType.MEETING_MINUTES,
                        UUID.randomUUID(),
                        idempotencyKey,
                        REQUESTED_AT);

        assertEquals(MailDeliveryStatus.REQUESTED, mail.deliveryStatus());
        assertEquals(idempotencyKey, mail.idempotencyKey());
        assertEquals(List.of(recipientOne, recipientTwo), mail.recipientUserIds());
        assertEquals(3, mail.mailboxEntries().size());
        assertTrue(
                mail.mailboxEntries().stream()
                        .anyMatch(
                                entry ->
                                        entry.ownerUserId().equals(senderUserId)
                                                && entry.mailboxType() == MailboxType.SENT));
    }

    @Test
    void requestMailFailsWhenRecipientIsDuplicated() {
        UUID recipientUserId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                Mail.request(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        List.of(recipientUserId, recipientUserId),
                                        "제목",
                                        "본문",
                                        MailType.NORMAL,
                                        MailBodyType.TEXT,
                                        null,
                                        null,
                                        UUID.randomUUID(),
                                        REQUESTED_AT));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void requestMailFailsWhenIdempotencyKeyIsNull() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                Mail.request(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        List.of(UUID.randomUUID()),
                                        "제목",
                                        "본문",
                                        MailType.NORMAL,
                                        MailBodyType.TEXT,
                                        null,
                                        null,
                                        null,
                                        REQUESTED_AT));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void markSentChangesRequestedMailToSent() {
        Mail mail = createTextMail();
        Instant sentAt = Instant.parse("2026-06-08T01:01:00Z");

        mail.markSent(sentAt);

        assertEquals(MailDeliveryStatus.SENT, mail.deliveryStatus());
        assertEquals(sentAt, mail.sentAt());
        assertNull(mail.failedAt());
    }

    @Test
    void deliveryResultCannotBeChangedTwice() {
        Mail mail = createTextMail();
        mail.markSent(Instant.parse("2026-06-08T01:01:00Z"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> mail.markFailed(Instant.parse("2026-06-08T01:02:00Z"), "FAILED"));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void invalidDeliveryTimeDoesNotPartiallyChangeStatus() {
        Mail mail = createTextMail();

        assertThrows(
                BusinessException.class,
                () -> mail.markSent(Instant.parse("2026-06-08T00:59:00Z")));

        assertEquals(MailDeliveryStatus.REQUESTED, mail.deliveryStatus());
        assertNull(mail.sentAt());
    }

    @Test
    void addAttachmentRejectsDuplicatedObjectKey() {
        Mail mail = createTextMail();
        UUID uploaderUserId = mail.senderUserId();
        MailAttachment first =
                MailAttachment.create(
                        uploaderUserId,
                        "attachments/mail/file.pdf",
                        "회의록.pdf",
                        "file.pdf",
                        "application/pdf",
                        1024);
        MailAttachment duplicated =
                MailAttachment.create(
                        uploaderUserId,
                        "attachments/mail/file.pdf",
                        "다른이름.pdf",
                        "file.pdf",
                        "application/pdf",
                        2048);
        mail.addAttachment(first);

        assertThrows(BusinessException.class, () -> mail.addAttachment(duplicated));
    }

    @Test
    void addAttachmentRejectsNonSenderUploader() {
        Mail mail = createTextMail();
        MailAttachment attachment =
                MailAttachment.create(
                        UUID.randomUUID(),
                        "attachments/mail/file.pdf",
                        "회의록.pdf",
                        "file.pdf",
                        "application/pdf",
                        1024);

        assertThrows(BusinessException.class, () -> mail.addAttachment(attachment));
    }

    @Test
    void returnedCollectionsCannotModifyAggregate() {
        Mail mail = createTextMail();

        assertThrows(
                UnsupportedOperationException.class,
                () -> mail.mailboxEntries().add(MailboxEntry.inbox(UUID.randomUUID())));
        assertFalse(mail.mailboxEntries().isEmpty());
    }

    private static Mail createTextMail() {
        return Mail.request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                "제목",
                "본문",
                MailType.NORMAL,
                MailBodyType.TEXT,
                null,
                null,
                UUID.randomUUID(),
                REQUESTED_AT);
    }
}
