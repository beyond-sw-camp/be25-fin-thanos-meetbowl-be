package com.meetbowl.application.mail;

import java.util.List;
import java.util.UUID;

public record SendMailCommand(
        UUID organizationId,
        UUID senderUserId,
        List<UUID> recipientUserIds,
        String subject,
        String body,
        String bodyType,
        String relatedResourceType,
        UUID relatedResourceId,
        UUID idempotencyKey) {}
