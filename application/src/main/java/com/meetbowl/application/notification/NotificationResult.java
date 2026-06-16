package com.meetbowl.application.notification;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.notification.Notification;

/**
 * 알림 한 건의 조회/발송 결과다.
 *
 * <p>type/resourceType은 도메인 enum을 그대로 노출하지 않고 이름 문자열로 변환해 담는다 — API 계층(app-api)이 domain에 의존하지 않도록 하는
 * 경계이며, 메일 결과 타입과 같은 규칙이다. resourceType/resourceId는 딥링크가 없는 알림이면 함께 null이다.
 */
public record NotificationResult(
        UUID id,
        String type,
        String title,
        String content,
        String resourceType,
        UUID resourceId,
        boolean read,
        Instant readAt,
        Instant createdAt) {

    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.id(),
                notification.type().name(),
                notification.title(),
                notification.content(),
                notification.resourceType() == null ? null : notification.resourceType().name(),
                notification.resourceId(),
                notification.isRead(),
                notification.readAt(),
                notification.createdAt());
    }
}