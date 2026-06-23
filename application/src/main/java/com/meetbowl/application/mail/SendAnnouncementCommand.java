package com.meetbowl.application.mail;

import java.util.UUID;

/** 공지 메일 발송 요청 명령이다. 수신자는 관리자가 지정하지 않고 발신자와 같은 조직의 활성 사용자로 서버가 계산하므로, 수신자 목록을 포함하지 않는다. */
public record SendAnnouncementCommand(
        UUID organizationId,
        UUID senderUserId,
        String subject,
        String body,
        String bodyType,
        UUID idempotencyKey) {}
