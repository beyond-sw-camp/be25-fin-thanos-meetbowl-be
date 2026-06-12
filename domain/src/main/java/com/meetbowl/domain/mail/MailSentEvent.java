package com.meetbowl.domain.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 내부 메일 발송이 완료되었음을 알리는 도메인 이벤트다.
 *
 * <p>루트 event-contract의 `mail.sent` payload에 대응한다. 본문이나 수신자 개인정보 본문은 담지 않고, 추적과 후속 처리에 필요한 식별자와 시각만
 * 전달한다.
 */
public record MailSentEvent(
        UUID mailId, UUID organizationId, List<UUID> recipientUserIds, Instant sentAt) {

    public MailSentEvent {
        // 발행 이후 호출자가 수신자 목록을 바꿔 이벤트 계약이 달라지는 것을 막는다.
        recipientUserIds = List.copyOf(recipientUserIds);
    }
}
