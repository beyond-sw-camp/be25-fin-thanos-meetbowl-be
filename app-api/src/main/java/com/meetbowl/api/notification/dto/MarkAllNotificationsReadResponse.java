package com.meetbowl.api.notification.dto;

import com.meetbowl.application.notification.MarkAllNotificationsReadResult;

/** 전체 읽음 처리 응답 본문이다. 이번에 읽음 처리된 건수와 처리 후 남은 안 읽은 알림 수를 담는다. */
public record MarkAllNotificationsReadResponse(int updatedCount, long unreadCount) {

    public static MarkAllNotificationsReadResponse from(MarkAllNotificationsReadResult result) {
        return new MarkAllNotificationsReadResponse(result.updatedCount(), result.unreadCount());
    }
}