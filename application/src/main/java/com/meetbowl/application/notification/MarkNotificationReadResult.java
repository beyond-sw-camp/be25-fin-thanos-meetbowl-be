package com.meetbowl.application.notification;

/** 단건 읽음 처리 결과다. 갱신된 알림과 처리 후 남은 안 읽은 알림 수(배지 갱신용)를 함께 담는다. */
public record MarkNotificationReadResult(NotificationResult notification, long unreadCount) {}