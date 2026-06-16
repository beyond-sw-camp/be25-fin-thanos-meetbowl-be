package com.meetbowl.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationType;

/** 단건 읽음 처리 UseCase의 소유권 검증(404/403)과 읽음 처리·unreadCount 반환을 확인한다. */
class MarkNotificationReadUseCaseTest {

    private final Instant now = Instant.parse("2099-01-01T00:00:00Z");
    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final MarkNotificationReadUseCase useCase =
            new MarkNotificationReadUseCase(repository, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void marksOwnedNotificationReadAndReturnsUnreadCount() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification owned =
                Notification.of(
                        notificationId,
                        userId,
                        NotificationType.MEETING_REMINDER,
                        "회의 10분 전",
                        "회의가 곧 시작됩니다.",
                        null,
                        null,
                        null,
                        Instant.parse("2098-12-31T00:00:00Z"));
        when(repository.findById(notificationId)).thenReturn(Optional.of(owned));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.countUnreadByRecipientUserId(userId)).thenReturn(0L);

        MarkNotificationReadResult result = useCase.execute(notificationId, userId);

        assertThat(result.notification().read()).isTrue();
        assertThat(result.notification().readAt()).isEqualTo(now);
        assertThat(result.unreadCount()).isZero();
    }

    @Test
    void throwsNotFoundWhenMissing() {
        UUID notificationId = UUID.randomUUID();
        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(notificationId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    void throwsForbiddenWhenOwnedByAnother() {
        UUID notificationId = UUID.randomUUID();
        Notification othersNotification =
                Notification.of(
                        notificationId,
                        UUID.randomUUID(),
                        NotificationType.MEETING_REMINDER,
                        "남의 알림",
                        "내용",
                        null,
                        null,
                        null,
                        now);
        when(repository.findById(notificationId)).thenReturn(Optional.of(othersNotification));

        assertThatThrownBy(() -> useCase.execute(notificationId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_FORBIDDEN_ACCESS);
        verify(repository, never()).save(any());
    }
}
