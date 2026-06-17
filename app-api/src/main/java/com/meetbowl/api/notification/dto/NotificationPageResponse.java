package com.meetbowl.api.notification.dto;

import java.util.List;

import com.meetbowl.application.notification.NotificationPageResult;

/** 알림 목록 페이지 응답 본문이다. 항목·페이지 메타에 더해 화면 배지용 안 읽은 알림 수를 함께 노출한다. */
public record NotificationPageResponse(
        List<NotificationResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unreadCount) {

    public static NotificationPageResponse from(NotificationPageResult result) {
        return new NotificationPageResponse(
                result.items().stream().map(NotificationResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.unreadCount());
    }
}
