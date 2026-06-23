package com.meetbowl.api.mail.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.mail.MailDetailResult;
import com.meetbowl.application.mail.MailSummaryResult;

/**
 * 메일 응답 DTO 모음이다.
 *
 * <p>목록용 {@link Summary}와 상세용 {@link Detail}을 한 네임스페이스로 묶는다. 목록은 본문을 빼 가볍게, 상세는 본문과 메일함 상태까지 담아 무겁게
 * 구성해 화면 용도에 맞춘다.
 */
public final class MailResponse {

    private MailResponse() {}

    public record Summary(
            UUID mailId,
            UUID senderUserId,
            List<UUID> recipientUserIds,
            String subject,
            String mailType,
            String deliveryStatus,
            String mailboxType,
            boolean read,
            boolean trashed,
            Instant requestedAt) {

        public static Summary from(MailSummaryResult result) {
            return new Summary(
                    result.mailId(),
                    result.senderUserId(),
                    result.recipientUserIds(),
                    result.subject(),
                    result.mailType(),
                    result.deliveryStatus(),
                    result.mailboxType(),
                    result.read(),
                    result.trashed(),
                    result.requestedAt());
        }
    }

    public record Detail(
            UUID mailId,
            UUID organizationId,
            UUID senderUserId,
            List<UUID> recipientUserIds,
            String subject,
            String body,
            String mailType,
            String bodyType,
            String relatedResourceType,
            UUID relatedResourceId,
            String deliveryStatus,
            String mailboxType,
            Instant requestedAt,
            Instant sentAt,
            boolean read,
            Instant readAt,
            boolean trashed,
            Instant trashedAt,
            List<Attachment> attachments) {

        public static Detail from(MailDetailResult result) {
            return new Detail(
                    result.mailId(),
                    result.organizationId(),
                    result.senderUserId(),
                    result.recipientUserIds(),
                    result.subject(),
                    result.body(),
                    result.mailType(),
                    result.bodyType(),
                    result.relatedResourceType(),
                    result.relatedResourceId(),
                    result.deliveryStatus(),
                    result.mailboxType(),
                    result.requestedAt(),
                    result.sentAt(),
                    result.read(),
                    result.readAt(),
                    result.trashed(),
                    result.trashedAt(),
                    result.attachments().stream().map(Attachment::from).toList());
        }

        /** 상세 응답의 첨부 메타데이터(다운로드는 별도 API). */
        public record Attachment(
                UUID attachmentId, String originalFileName, String mimeType, long sizeBytes) {

            public static Attachment from(com.meetbowl.application.mail.MailAttachmentInfo info) {
                return new Attachment(
                        info.attachmentId(),
                        info.originalFileName(),
                        info.mimeType(),
                        info.sizeBytes());
            }
        }
    }
}
