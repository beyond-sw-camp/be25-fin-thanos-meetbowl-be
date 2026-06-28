package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 메일함 목록의 한 항목 요약이다. 본문은 빼고 목록 표시에 필요한 메타와 내 메일함 상태(읽음·휴지통)만 담는다. */
public record MailSummaryResult(
        UUID mailId,
        UUID senderUserId,
        List<UUID> recipientUserIds,
        List<ExternalMailRecipientResult> externalRecipients,
        String subject,
        String mailType,
        String deliveryStatus,
        String mailboxType,
        boolean read,
        boolean trashed,
        Instant requestedAt) {}
