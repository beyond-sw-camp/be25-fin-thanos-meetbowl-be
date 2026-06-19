package com.meetbowl.application.notification;

import java.util.List;

/**
 * 알림 목록의 한 페이지다. 항목과 페이지 메타(현재 페이지·크기·전체 개수·전체 페이지 수)에 더해, 화면 배지 표시용 안 읽은 알림 수(unreadCount)를 함께
 * 담는다.
 */
public record NotificationPageResult(
        List<NotificationResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unreadCount) {}
