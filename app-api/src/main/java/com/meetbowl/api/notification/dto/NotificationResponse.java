package com.meetbowl.api.notification.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.notification.NotificationResult;

/**
 * 알림 한 건의 응답 본문이다.
 *
 * <p>목록·읽음 처리·실시간(SSE) 전달에서 모두 같은 형태를 쓴다. resourceType/resourceId는 클릭 시 원본 회의/회의록으로 이동(딥링크)하기 위한 정보이며
 * 딥링크가 없는 알림이면 둘 다 null이다. 시각은 ISO-8601 UTC로 직렬화된다.
 */
public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String content,
        String resourceType,
        UUID resourceId,
        boolean read,
        Instant readAt,
        Instant createdAt) {

    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.id(),
                result.type(),
                result.title(),
                result.content(),
                result.resourceType(),
                result.resourceId(),
                result.read(),
                result.readAt(),
                result.createdAt());
    }
}