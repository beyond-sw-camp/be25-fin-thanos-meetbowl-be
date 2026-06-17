package com.meetbowl.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationType;

/** 목록 조회 UseCase의 offset 환산·전체 페이지 수 계산·unreadCount 포함을 확인한다. */
class ListNotificationsUseCaseTest {

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final ListNotificationsUseCase useCase = new ListNotificationsUseCase(repository);

    @Test
    void mapsPageMetaAndUnreadCount() {
        UUID userId = UUID.randomUUID();
        Notification item =
                Notification.of(
                        UUID.randomUUID(),
                        userId,
                        NotificationType.MEETING_REMINDER,
                        "회의 10분 전",
                        "회의가 곧 시작됩니다.",
                        null,
                        null,
                        null,
                        Instant.parse("2099-01-01T00:00:00Z"));
        // page=2, size=2 → offset=2 로 위임되어야 한다.
        when(repository.findPageByRecipientUserId(userId, 2, 2)).thenReturn(List.of(item));
        when(repository.countByRecipientUserId(userId)).thenReturn(5L);
        when(repository.countUnreadByRecipientUserId(userId)).thenReturn(3L);

        NotificationPageResult result = useCase.list(userId, 2, 2);

        assertThat(result.items()).hasSize(1);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3); // ceil(5/2)
        assertThat(result.unreadCount()).isEqualTo(3);
        verify(repository).findPageByRecipientUserId(userId, 2, 2);
    }
}
