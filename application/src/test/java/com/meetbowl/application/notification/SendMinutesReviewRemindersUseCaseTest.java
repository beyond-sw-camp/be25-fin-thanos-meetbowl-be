package com.meetbowl.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;
import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

/**
 * 회의록 검토 미완료 재알림(방법 A) 로직을 확인한다. 기준시각 재사용·검토 완료 시 중단·주기 도래 판단을 시간 고정 Clock으로 검증한다.
 */
class SendMinutesReviewRemindersUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-16T09:00:00Z");

    private final NotificationRepositoryPort notificationPort =
            mock(NotificationRepositoryPort.class);
    private final MinutesRepositoryPort minutesPort = mock(MinutesRepositoryPort.class);
    private final UserSettingRepositoryPort userSettingPort =
            mock(UserSettingRepositoryPort.class);
    private final DispatchNotificationUseCase dispatch = mock(DispatchNotificationUseCase.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SendMinutesReviewRemindersUseCase useCase =
            new SendMinutesReviewRemindersUseCase(
                    notificationPort, minutesPort, userSettingPort, dispatch, clock);

    @Test
    void sendsReminderWhenPeriodElapsedSinceRequestAndStillInReview() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        // 요청이 90분 전, 주기 60분 → 첫 주기(요청+60분) 도래.
        givenReviewRequest(userId, minutesId, NOW.minus(Duration.ofMinutes(90)));
        givenMinutesStatus(minutesId, MinutesStatus.IN_REVIEW);
        givenReminderPeriodMinutes(userId, 60);
        givenNoPreviousReminder(userId, minutesId);

        int sent = useCase.run();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<DispatchNotificationCommand> captor =
                ArgumentCaptor.forClass(DispatchNotificationCommand.class);
        verify(dispatch).execute(captor.capture());
        assertThat(captor.getValue().type())
                .isEqualTo(NotificationType.MINUTES_REVIEW_REMINDER.name());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(userId);
        assertThat(captor.getValue().resourceId()).isEqualTo(minutesId);
        assertThat(captor.getValue().resourceType())
                .isEqualTo(NotificationResourceType.MEETING_MINUTES.name());
    }

    @Test
    void doesNotSendBeforePeriodElapses() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        // 요청이 30분 전, 주기 60분 → 아직 도래 전.
        givenReviewRequest(userId, minutesId, NOW.minus(Duration.ofMinutes(30)));
        givenMinutesStatus(minutesId, MinutesStatus.IN_REVIEW);
        givenReminderPeriodMinutes(userId, 60);
        givenNoPreviousReminder(userId, minutesId);

        int sent = useCase.run();

        assertThat(sent).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void stopsWhenMinutesNoLongerInReview() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        givenReviewRequest(userId, minutesId, NOW.minus(Duration.ofHours(5)));
        // 이미 승인된 회의록은 재촉하지 않는다.
        givenMinutesStatus(minutesId, MinutesStatus.APPROVED);

        int sent = useCase.run();

        assertThat(sent).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void usesLastReminderAsBaselineForNextPeriod() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        // 요청은 오래됐지만, 마지막 재알림이 30분 전이고 주기 60분이면 아직 다음 주기 전.
        givenReviewRequest(userId, minutesId, NOW.minus(Duration.ofHours(5)));
        givenMinutesStatus(minutesId, MinutesStatus.IN_REVIEW);
        givenReminderPeriodMinutes(userId, 60);
        when(notificationPort.findLatestByRecipientUserIdAndTypeAndResourceId(
                        userId, NotificationType.MINUTES_REVIEW_REMINDER, minutesId))
                .thenReturn(
                        Optional.of(
                                reminder(userId, minutesId, NOW.minus(Duration.ofMinutes(30)))));

        int sent = useCase.run();

        assertThat(sent).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void sendsAtExactPeriodBoundary() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        // 요청이 정확히 주기(60분) 전 → 경계(now == dueAt)에서 발송한다.
        givenReviewRequest(userId, minutesId, NOW.minus(Duration.ofMinutes(60)));
        givenMinutesStatus(minutesId, MinutesStatus.IN_REVIEW);
        givenReminderPeriodMinutes(userId, 60);
        givenNoPreviousReminder(userId, minutesId);

        assertThat(useCase.run()).isEqualTo(1);
        verify(dispatch).execute(any());
    }

    @Test
    void skipsWhenMinutesNotFound() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        givenReviewRequest(userId, minutesId, NOW.minus(Duration.ofHours(5)));
        // 회의록이 삭제돼 조회되지 않으면 건너뛰고 예외 없이 진행한다.
        when(minutesPort.findById(minutesId)).thenReturn(Optional.empty());

        assertThat(useCase.run()).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void skipsRequestWithoutResource() {
        UUID userId = UUID.randomUUID();
        // 연결 리소스가 없는 검토 요청 알림(딥링크 없음)은 회의록을 특정할 수 없어 건너뛴다.
        when(notificationPort.findByType(NotificationType.MINUTES_REVIEW_REQUEST))
                .thenReturn(
                        List.of(
                                Notification.of(
                                        UUID.randomUUID(),
                                        userId,
                                        NotificationType.MINUTES_REVIEW_REQUEST,
                                        "회의록 검토 요청",
                                        "검토를 요청합니다.",
                                        null,
                                        null,
                                        null,
                                        NOW.minus(Duration.ofHours(5)))));

        assertThat(useCase.run()).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void dedupesMultipleRequestRowsForSameRecipientAndMinutes() {
        UUID userId = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        // 같은 (수신자, 회의록)에 검토 요청 알림이 두 건이어도 한 주기에 한 번만 보낸다.
        when(notificationPort.findByType(NotificationType.MINUTES_REVIEW_REQUEST))
                .thenReturn(
                        List.of(
                                reviewRequest(userId, minutesId, NOW.minus(Duration.ofHours(3))),
                                reviewRequest(userId, minutesId, NOW.minus(Duration.ofHours(2)))));
        givenMinutesStatus(minutesId, MinutesStatus.IN_REVIEW);
        givenReminderPeriodMinutes(userId, 60);
        givenNoPreviousReminder(userId, minutesId);

        assertThat(useCase.run()).isEqualTo(1);
        verify(dispatch, times(1)).execute(any());
    }

    private void givenReviewRequest(UUID userId, UUID minutesId, Instant requestedAt) {
        when(notificationPort.findByType(NotificationType.MINUTES_REVIEW_REQUEST))
                .thenReturn(List.of(reviewRequest(userId, minutesId, requestedAt)));
    }

    private void givenMinutesStatus(UUID minutesId, MinutesStatus status) {
        when(minutesPort.findById(minutesId)).thenReturn(Optional.of(minutesWithStatus(status)));
    }

    private void givenReminderPeriodMinutes(UUID userId, int minutes) {
        when(userSettingPort.findByUserId(userId))
                .thenReturn(Optional.of(userSetting(userId, minutes)));
    }

    private void givenNoPreviousReminder(UUID userId, UUID minutesId) {
        when(notificationPort.findLatestByRecipientUserIdAndTypeAndResourceId(
                        eq(userId), eq(NotificationType.MINUTES_REVIEW_REMINDER), eq(minutesId)))
                .thenReturn(Optional.empty());
    }

    private Notification reviewRequest(UUID userId, UUID minutesId, Instant createdAt) {
        return Notification.of(
                UUID.randomUUID(),
                userId,
                NotificationType.MINUTES_REVIEW_REQUEST,
                "회의록 검토 요청",
                "검토를 요청합니다.",
                NotificationResourceType.MEETING_MINUTES,
                minutesId,
                null,
                createdAt);
    }

    private Notification reminder(UUID userId, UUID minutesId, Instant createdAt) {
        return Notification.of(
                UUID.randomUUID(),
                userId,
                NotificationType.MINUTES_REVIEW_REMINDER,
                "회의록 검토 미완료 알림",
                "검토를 진행해 주세요.",
                NotificationResourceType.MEETING_MINUTES,
                minutesId,
                null,
                createdAt);
    }

    private Minutes minutesWithStatus(MinutesStatus status) {
        return Minutes.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                "요약",
                "본문",
                "model",
                "v1",
                null,
                null,
                null);
    }

    private UserSetting userSetting(UUID userId, int minutesReviewReminderMinutes) {
        return new UserSetting(
                UUID.randomUUID(),
                userId,
                UserSetting.DEFAULT_MEETING_REMINDER_MINUTES_BEFORE,
                minutesReviewReminderMinutes,
                NOW,
                NOW);
    }
}