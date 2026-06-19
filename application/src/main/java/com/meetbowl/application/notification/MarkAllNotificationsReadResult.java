package com.meetbowl.application.notification;

/**
 * 전체 읽음 처리 결과다. 이번에 새로 읽음 처리된 건수(updatedCount)와 처리 후 남은 안 읽은 알림 수(unreadCount)를 담는다. 정상 처리 시
 * unreadCount는 0이지만, 처리 도중 새 알림이 도착할 수 있어 응답에 명시적으로 포함한다.
 */
public record MarkAllNotificationsReadResult(int updatedCount, long unreadCount) {}
