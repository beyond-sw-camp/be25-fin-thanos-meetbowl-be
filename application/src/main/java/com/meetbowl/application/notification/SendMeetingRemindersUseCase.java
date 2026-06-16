package com.meetbowl.application.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.notification.MeetingReminderTarget;
import com.meetbowl.domain.notification.MeetingReminderTargetPort;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;
import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

/**
 * 회의 시작 전 리마인더(MEETING_REMINDER) 발송 스케줄러 로직이다.
 *
 * <p>각 참석자는 개인 설정의 알림 시간(분)이 달라 발송 시각도 제각각이므로, 곧 시작할 예약 회의를 한 번 모은 뒤 참석자별로 {@code 발송시각 =
 * 회의 예정시작 − 알림시간(분)}을 계산해 도래한 사람에게만 보낸다. 같은 회의·같은 사람에게 두 번 보내지 않도록, 이미 발송된 MEETING_REMINDER가 있으면
 * 건너뛴다(알림 테이블이 발송 원장 역할을 겸한다).
 *
 * <p>개인 설정이 없는 사용자는 {@link UserSetting#DEFAULT_MEETING_REMINDER_MINUTES_BEFORE} 기본값을 적용한다. 발송 자체는
 * {@link DispatchNotificationUseCase}에 위임해 저장·실시간 전달·트랜잭션 경계를 일관되게 처리한다.
 */
@Service
public class SendMeetingRemindersUseCase {

    /**
     * 한 번에 미리 가져오는 회의의 시작 시각 상한(현재 시각 기준). 참석자 알림 시간(분)이 이 범위를 넘으면 그만큼 일찍은 알릴 수 없으나, 실제 설정값은 분~수십
     * 분 단위라 하루면 충분하다. 범위를 넓힐수록 매 주기 조회량이 늘어난다.
     */
    private static final Duration LOOKAHEAD = Duration.ofDays(1);

    private final MeetingReminderTargetPort meetingReminderTargetPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final UserSettingRepositoryPort userSettingRepositoryPort;
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;
    private final Clock clock;

    public SendMeetingRemindersUseCase(
            MeetingReminderTargetPort meetingReminderTargetPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            UserSettingRepositoryPort userSettingRepositoryPort,
            NotificationRepositoryPort notificationRepositoryPort,
            DispatchNotificationUseCase dispatchNotificationUseCase,
            Clock clock) {
        this.meetingReminderTargetPort = meetingReminderTargetPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.userSettingRepositoryPort = userSettingRepositoryPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
        this.clock = clock;
    }

    /** 주기 호출 진입점. 발송 도래한 (회의, 참석자) 조합 수를 반환한다(로깅/테스트 확인용). */
    public int run() {
        Instant now = Instant.now(clock);
        List<MeetingReminderTarget> meetings =
                meetingReminderTargetPort.findScheduledStartingWithin(now, now.plus(LOOKAHEAD));

        int sent = 0;
        for (MeetingReminderTarget meeting : meetings) {
            for (MeetingAttendee attendee :
                    meetingAttendeeRepositoryPort.findByMeetingId(meeting.meetingId())) {
                if (sendIfDue(now, meeting, attendee.userId())) {
                    sent++;
                }
            }
        }
        return sent;
    }

    private boolean sendIfDue(Instant now, MeetingReminderTarget meeting, UUID userId) {
        int minutesBefore = reminderMinutesBefore(userId);
        Instant remindAt = meeting.scheduledAt().minus(Duration.ofMinutes(minutesBefore));
        if (now.isBefore(remindAt)) {
            return false; // 아직 발송 시각 전이다.
        }
        if (alreadyReminded(userId, meeting.meetingId())) {
            return false; // 같은 회의·같은 사람에게 이미 보냈다.
        }
        dispatchNotificationUseCase.execute(
                new DispatchNotificationCommand(
                        userId,
                        NotificationType.MEETING_REMINDER.name(),
                        "회의 시작 알림",
                        "곧 \"" + meeting.title() + "\" 회의가 시작됩니다.",
                        NotificationResourceType.MEETING.name(),
                        meeting.meetingId()));
        return true;
    }

    private boolean alreadyReminded(UUID userId, UUID meetingId) {
        return notificationRepositoryPort
                .findLatestByRecipientUserIdAndTypeAndResourceId(
                        userId, NotificationType.MEETING_REMINDER, meetingId)
                .isPresent();
    }

    private int reminderMinutesBefore(UUID userId) {
        return userSettingRepositoryPort
                .findByUserId(userId)
                .map(UserSetting::meetingReminderMinutesBefore)
                .orElse(UserSetting.DEFAULT_MEETING_REMINDER_MINUTES_BEFORE);
    }
}