package com.meetbowl.application.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.notification.NotificationRepositoryPort;

/**
 * 수신자 본인의 알림 목록을 페이지로 조회한다.
 *
 * <p>항상 최신순이며, 페이지 항목과 함께 전체 개수(페이지 계산용)와 안 읽은 알림 수(화면 배지용)를 함께 돌려준다. page는 1부터 시작한다.
 */
@Service
public class ListNotificationsUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;

    public ListNotificationsUseCase(NotificationRepositoryPort notificationRepositoryPort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    @Transactional(readOnly = true)
    public NotificationPageResult list(UUID recipientUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<NotificationResult> items =
                notificationRepositoryPort
                        .findPageByRecipientUserId(recipientUserId, offset, size)
                        .stream()
                        .map(NotificationResult::from)
                        .toList();

        long totalElements = notificationRepositoryPort.countByRecipientUserId(recipientUserId);
        long unreadCount = notificationRepositoryPort.countUnreadByRecipientUserId(recipientUserId);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new NotificationPageResult(
                items, page, size, totalElements, totalPages, unreadCount);
    }
}
