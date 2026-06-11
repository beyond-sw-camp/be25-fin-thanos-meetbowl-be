package com.meetbowl.application.mail;

import java.util.List;
import java.util.UUID;

/**
 * 시스템 내부 메일 발송 요청의 application 계약이다.
 *
 * <p>내부 호출자(다른 서버/시스템 흐름)가 신뢰된 토큰으로 전달하므로 발신자/조직 ID를 본문으로 받는다. 사용자 발송과 달리 화면 인증 사용자에서 채우지 않는다.
 */
public record DispatchInternalMailCommand(
        UUID organizationId,
        UUID senderUserId,
        List<UUID> recipientUserIds,
        String subject,
        String body,
        String bodyType,
        String relatedResourceType,
        UUID relatedResourceId,
        UUID idempotencyKey) {}
