package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MailSummaryResult(
        UUID mailId,
        UUID senderUserId,
        List<UUID> recipientUserIds,
        String subject,
        String mailType,
        String deliveryStatus,
        String mailboxType,
        boolean read,
        boolean trashed,
        Instant requestedAt) {}
