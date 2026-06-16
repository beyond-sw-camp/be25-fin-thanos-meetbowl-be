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

import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.notification.MeetingReminderTarget;
import com.meetbowl.domain.notification.MeetingReminderTargetPort;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationType;
import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

/**
 * 회의 시작 전 리마인더 발송 스케줄러 로직을 확인한다. 발송 시각 도래·중복 방지·개인 설정 적용을 시간 고정 Clock으로 결정적으로 검증한다.
 */
class SendMeetingRemindersUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-16T09:00:00Z");

    private final MeetingReminderTargetPort meetingPort = mock(MeetingReminderTargetPort.class);
    private final MeetingAttendeeRepositoryPort attendeePort =
            mock(MeetingAttendeeRepositoryPort.class);
    private final UserSettingRepositoryPort userSettingPort =
            mock(UserSettingRepositoryPort.class);
    private final NotificationRepositoryPort notificationPort =
            mock(NotificationRepositoryPort.class);
    private final DispatchNotificationUseCase dispatch = mock(DispatchNotificationUseCase.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final SendMeetingRemindersUseCase useCase =
            new SendMeetingRemindersUseCase(
                    meetingPort, attendeePort, userSettingPort, notificationPort, dispatch, clock);

    @Test
    void sendsReminderWhenDueAndNotYetSent() {
        UUID meetingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // 10분 뒤 시작, 개인 설정 10분 전 → 지금이 발송 시각.
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(10)));
        givenAttendees(meetingId, attendee(meetingId, userId));
        givenReminderMinutesBefore(userId, 10);
        givenNoPreviousReminder(userId, meetingId);

        int sent = useCase.run();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<DispatchNotificationCommand> captor =
                ArgumentCaptor.forClass(DispatchNotificationCommand.class);
        verify(dispatch).execute(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.MEETING_REMINDER.name());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(userId);
        assertThat(captor.getValue().resourceId()).isEqualTo(meetingId);
    }

    @Test
    void doesNotSendBeforeReminderTime() {
        UUID meetingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // 30분 뒤 시작, 10분 전 알림 → 아직 20분 남아 발송 시각 전.
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(30)));
        givenAttendees(meetingId, attendee(meetingId, userId));
        givenReminderMinutesBefore(userId, 10);

        int sent = useCase.run();

        assertThat(sent).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void doesNotResendWhenReminderAlreadyExists() {
        UUID meetingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(5)));
        givenAttendees(meetingId, attendee(meetingId, userId));
        givenReminderMinutesBefore(userId, 10);
        // 이미 보낸 기록이 있으면 다시 보내지 않는다.
        when(notificationPort.findLatestByRecipientUserIdAndTypeAndResourceId(
                        userId, NotificationType.MEETING_REMINDER, meetingId))
                .thenReturn(Optional.of(mock(com.meetbowl.domain.notification.Notification.class)));

        int sent = useCase.run();

        assertThat(sent).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void usesDefaultMinutesWhenUserHasNoSetting() {
        UUID meetingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // 기본값(10분 전)이 적용되면 10분 뒤 시작 회의는 지금 발송 시각이다.
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(10)));
        givenAttendees(meetingId, attendee(meetingId, userId));
        when(userSettingPort.findByUserId(userId)).thenReturn(Optional.empty());
        givenNoPreviousReminder(userId, meetingId);

        int sent = useCase.run();

        assertThat(sent).isEqualTo(1);
        verify(dispatch).execute(any());
    }

    @Test
    void sendsAtExactReminderBoundary() {
        UUID meetingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // 발송 시각(시작−10분)이 정확히 지금인 경계에서 발송한다.
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(10)));
        givenAttendees(meetingId, attendee(meetingId, userId));
        givenReminderMinutesBefore(userId, 10);
        givenNoPreviousReminder(userId, meetingId);

        assertThat(useCase.run()).isEqualTo(1);
        verify(dispatch).execute(any());
    }

    @Test
    void sendsOnlyToDueAttendeesWhenOffsetsDiffer() {
        UUID meetingId = UUID.randomUUID();
        UUID dueUser = UUID.randomUUID();
        UUID notDueUser = UUID.randomUUID();
        // 30분 뒤 시작. dueUser는 30분 전 알림(=지금 도래), notDueUser는 10분 전 알림(아직 20분 남음).
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(30)));
        givenAttendees(
                meetingId, attendee(meetingId, dueUser), attendee(meetingId, notDueUser));
        givenReminderMinutesBefore(dueUser, 30);
        givenReminderMinutesBefore(notDueUser, 10);
        givenNoPreviousReminder(dueUser, meetingId);
        givenNoPreviousReminder(notDueUser, meetingId);

        assertThat(useCase.run()).isEqualTo(1);
        ArgumentCaptor<DispatchNotificationCommand> captor =
                ArgumentCaptor.forClass(DispatchNotificationCommand.class);
        verify(dispatch, times(1)).execute(captor.capture());
        // 도래한 참석자에게만 갔는지 수신자까지 확인한다.
        assertThat(captor.getValue().recipientUserId()).isEqualTo(dueUser);
    }

    @Test
    void doesNothingWhenNoUpcomingMeetings() {
        when(meetingPort.findScheduledStartingWithin(any(), any())).thenReturn(List.of());

        assertThat(useCase.run()).isZero();
        verify(dispatch, never()).execute(any());
    }

    @Test
    void doesNotCrashWhenMeetingHasNoAttendees() {
        UUID meetingId = UUID.randomUUID();
        givenMeetingStartingAt(meetingId, NOW.plus(Duration.ofMinutes(5)));
        when(attendeePort.findByMeetingId(meetingId)).thenReturn(List.of());

        assertThat(useCase.run()).isZero();
        verify(dispatch, never()).execute(any());
    }

    private void givenMeetingStartingAt(UUID meetingId, Instant scheduledAt) {
        when(meetingPort.findScheduledStartingWithin(any(), any()))
                .thenReturn(List.of(new MeetingReminderTarget(meetingId, "주간 회의", scheduledAt)));
    }

    private void givenAttendees(UUID meetingId, MeetingAttendee... attendees) {
        when(attendeePort.findByMeetingId(meetingId)).thenReturn(List.of(attendees));
    }

    private void givenReminderMinutesBefore(UUID userId, int minutes) {
        when(userSettingPort.findByUserId(userId))
                .thenReturn(Optional.of(userSetting(userId, minutes)));
    }

    private void givenNoPreviousReminder(UUID userId, UUID meetingId) {
        when(notificationPort.findLatestByRecipientUserIdAndTypeAndResourceId(
                        eq(userId), eq(NotificationType.MEETING_REMINDER), eq(meetingId)))
                .thenReturn(Optional.empty());
    }

    private MeetingAttendee attendee(UUID meetingId, UUID userId) {
        return MeetingAttendee.create(meetingId, userId, AttendeeRole.PARTICIPANT);
    }

    private UserSetting userSetting(UUID userId, int meetingReminderMinutesBefore) {
        return new UserSetting(
                UUID.randomUUID(),
                userId,
                meetingReminderMinutesBefore,
                UserSetting.DEFAULT_MINUTES_REVIEW_REMINDER_MINUTES,
                NOW,
                NOW);
    }
}