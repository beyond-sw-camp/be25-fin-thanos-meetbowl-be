package com.meetbowl.domain.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 내부 메일 발송이 실패했음을 알리는 도메인 이벤트다.
 *
 * <p>루트 event-contract의 `mail.failed` payload에 대응한다. {@code retryable}은 재시도로 회복 가능한 장애인지(인프라 오류 등)
 * 구분해, 소비자가 재처리 여부를 판단하는 근거로 사용한다.
 */
public record MailFailedEvent(
        UUID mailId,
        UUID organizationId,
        List<UUID> recipientUserIds,
        String failureCode,
        String failureReason,
        Instant failedAt,
        boolean retryable) {

    public MailFailedEvent {
        recipientUserIds = List.copyOf(recipientUserIds);
    }
}
