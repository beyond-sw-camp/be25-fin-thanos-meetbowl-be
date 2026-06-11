package com.meetbowl.api.mail.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.mail.MailDetailResult;
import com.meetbowl.application.mail.MailSummaryResult;

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
            Instant trashedAt) {

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
                    result.trashedAt());
        }
    }
}
