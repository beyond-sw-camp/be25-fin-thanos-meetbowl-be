package com.meetbowl.infrastructure.persistence.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/** 엔티티 ↔ 도메인 매핑이 필드를 누락/변형 없이 보존하는지(데이터 정합성) 검증한다. */
class NotificationEntityTest {

    @Test
    void roundTripPreservesAllFields() {
        UUID id = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant readAt = Instant.parse("2026-06-15T10:00:00Z");

        Notification origin =
                Notification.of(
                        id,
                        recipient,
                        NotificationType.MINUTES_REVIEW_REMINDER,
                        "검토 지연 알림",
                        "회의록 검토가 지연되고 있습니다.",
                        NotificationResourceType.MEETING_MINUTES,
                        resourceId,
                        readAt);

        Notification restored = NotificationEntity.from(origin).toDomain();

        assertEquals(id, restored.id());
        assertEquals(recipient, restored.recipientUserId());
        assertEquals(NotificationType.MINUTES_REVIEW_REMINDER, restored.type());
        assertEquals("검토 지연 알림", restored.title());
        assertEquals("회의록 검토가 지연되고 있습니다.", restored.content());
        assertEquals(NotificationResourceType.MEETING_MINUTES, restored.resourceType());
        assertEquals(resourceId, restored.resourceId());
        assertEquals(readAt, restored.readAt());
    }

    @Test
    void roundTripPreservesNullableFields() {
        // 딥링크 없음 + 미읽음(readAt null) 상태도 그대로 보존되어야 한다.
        Notification origin =
                Notification.create(
                        UUID.randomUUID(),
                        NotificationType.MEETING_CANCELLED,
                        "회의 취소",
                        "회의가 취소되었습니다.",
                        null,
                        null);

        Notification restored = NotificationEntity.from(origin).toDomain();

        assertNull(restored.resourceType());
        assertNull(restored.resourceId());
        assertNull(restored.readAt());
        assertEquals(NotificationType.MEETING_CANCELLED, restored.type());
    }
}
