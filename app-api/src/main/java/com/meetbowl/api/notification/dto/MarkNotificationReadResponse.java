package com.meetbowl.api.notification.dto;

import com.meetbowl.application.notification.MarkNotificationReadResult;

/** 단건 읽음 처리 응답 본문이다. 갱신된 알림과 처리 후 남은 안 읽은 알림 수를 함께 내려 화면 배지를 즉시 갱신할 수 있게 한다. */
public record MarkNotificationReadResponse(NotificationResponse notification, long unreadCount) {

    public static MarkNotificationReadResponse from(MarkNotificationReadResult result) {
        return new MarkNotificationReadResponse(
                NotificationResponse.from(result.notification()), result.unreadCount());
    }
}