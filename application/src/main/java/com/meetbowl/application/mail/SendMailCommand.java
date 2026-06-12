package com.meetbowl.application.mail;

import java.util.List;
import java.util.UUID;

/** 사용자 메일 발송 UseCase의 입력 계약이다. 발신자/조직 ID는 인증 사용자에서 채우고 본문으로 받지 않는다. */
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
