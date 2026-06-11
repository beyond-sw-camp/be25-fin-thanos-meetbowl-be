package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MailDetailResult(
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
        Instant trashedAt) {}
