package com.meetbowl.application.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;

/**
 * 수신자 본인의 안 읽은 알림을 모두 읽음 처리한다.
 *
 * <p>안 읽은 알림만 골라 갱신하므로 이미 읽은 알림의 첫 읽은 시각은 건드리지 않는다. updatedCount는 이번에 실제로 읽음 처리된 건수다.
 */
@Service
public class MarkAllNotificationsReadUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final Clock clock;

    @Autowired
    public MarkAllNotificationsReadUseCase(NotificationRepositoryPort notificationRepositoryPort) {
        this(notificationRepositoryPort, Clock.systemUTC());
    }

    MarkAllNotificationsReadUseCase(
            NotificationRepositoryPort notificationRepositoryPort, Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public MarkAllNotificationsReadResult execute(UUID recipientUserId) {
        List<Notification> unread =
                notificationRepositoryPort.findUnreadByRecipientUserId(recipientUserId);

        Instant now = Instant.now(clock);
        unread.forEach(
                notification -> {
                    notification.markRead(now);
                    notificationRepositoryPort.save(notification);
                });

        long unreadCount = notificationRepositoryPort.countUnreadByRecipientUserId(recipientUserId);
        return new MarkAllNotificationsReadResult(unread.size(), unreadCount);
    }
}
