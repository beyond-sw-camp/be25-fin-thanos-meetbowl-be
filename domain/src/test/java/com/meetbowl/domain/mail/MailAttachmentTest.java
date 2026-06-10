package com.meetbowl.domain.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class MailAttachmentTest {

    @Test
    void createAttachmentMetadata() {
        UUID uploaderUserId = UUID.randomUUID();

        MailAttachment attachment =
                MailAttachment.create(
                        uploaderUserId,
                        "attachments/mail-id/file-id.pdf",
                        "회의록.pdf",
                        "file-id.pdf",
                        "application/pdf",
                        1024);

        assertEquals(uploaderUserId, attachment.uploaderUserId());
        assertEquals("attachments/mail-id/file-id.pdf", attachment.objectKey());
        assertEquals(1024, attachment.sizeBytes());
    }

    @Test
    void createAttachmentFailsWhenSizeIsNotPositive() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                MailAttachment.create(
                                        UUID.randomUUID(),
                                        "attachments/mail-id/file-id.pdf",
                                        "회의록.pdf",
                                        "file-id.pdf",
                                        "application/pdf",
                                        0));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
