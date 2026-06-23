package com.meetbowl.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationType;

/** 전체 읽음 처리 UseCase가 안 읽은 알림만 읽음 처리하고 처리 건수/잔여 unreadCount를 반환하는지 확인한다. */
class MarkAllNotificationsReadUseCaseTest {

    private final Instant now = Instant.parse("2099-01-01T00:00:00Z");
    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final MarkAllNotificationsReadUseCase useCase =
            new MarkAllNotificationsReadUseCase(repository, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void marksAllUnreadAndReportsCounts() {
        UUID userId = UUID.randomUUID();
        Notification first = unread(userId, "알림1");
        Notification second = unread(userId, "알림2");
        when(repository.findUnreadByRecipientUserId(userId)).thenReturn(List.of(first, second));
        when(repository.countUnreadByRecipientUserId(userId)).thenReturn(0L);

        MarkAllNotificationsReadResult result = useCase.execute(userId);

        assertThat(result.updatedCount()).isEqualTo(2);
        assertThat(result.unreadCount()).isZero();
        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(repository, times(2)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsZeroWhenNothingUnread() {
        UUID userId = UUID.randomUUID();
        when(repository.findUnreadByRecipientUserId(userId)).thenReturn(List.of());
        when(repository.countUnreadByRecipientUserId(userId)).thenReturn(0L);

        MarkAllNotificationsReadResult result = useCase.execute(userId);

        assertThat(result.updatedCount()).isZero();
        assertThat(result.unreadCount()).isZero();
    }

    private Notification unread(UUID userId, String title) {
        return Notification.of(
                UUID.randomUUID(),
                userId,
                NotificationType.MEETING_REMINDER,
                title,
                "내용",
                null,
                null,
                null,
                now);
    }
}
