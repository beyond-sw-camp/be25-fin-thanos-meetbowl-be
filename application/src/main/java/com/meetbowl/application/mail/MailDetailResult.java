package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 메일 상세 조회 결과다. 공용 메일 본문과 현재 사용자의 메일함 상태(읽음·휴지통)를 함께 담는다. */
public record MailDetailResult(
        UUID mailId,
        UUID organizationId,
        UUID senderUserId,
        List<UUID> recipientUserIds,
        List<ExternalMailRecipientResult> externalRecipients,
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
        List<MailAttachmentInfo> attachments) {}
