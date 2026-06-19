package com.meetbowl.application.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;

/**
 * 알림 한 건을 읽음 처리한다.
 *
 * <p>수신자 본인만 처리할 수 있다 — 없는 알림은 404, 남의 알림이면 403으로 막아 다른 사용자의 알림 존재 여부가 새어 나가지 않게 한다. 이미 읽은 알림에 대한
 * 재호출은 도메인 {@code markRead}가 멱등 처리하므로 첫 읽은 시각이 보존된다.
 */
@Service
public class MarkNotificationReadUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final Clock clock;

    @Autowired
    public MarkNotificationReadUseCase(NotificationRepositoryPort notificationRepositoryPort) {
        this(notificationRepositoryPort, Clock.systemUTC());
    }

    MarkNotificationReadUseCase(
            NotificationRepositoryPort notificationRepositoryPort, Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public MarkNotificationReadResult execute(UUID notificationId, UUID recipientUserId) {
        Notification notification =
                notificationRepositoryPort
                        .findById(notificationId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwnedBy(recipientUserId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN_ACCESS);
        }

        notification.markRead(Instant.now(clock));
        Notification saved = notificationRepositoryPort.save(notification);

        long unreadCount = notificationRepositoryPort.countUnreadByRecipientUserId(recipientUserId);
        return new MarkNotificationReadResult(NotificationResult.from(saved), unreadCount);
    }
}
