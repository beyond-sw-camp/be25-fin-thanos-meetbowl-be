package com.meetbowl.application.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 회의 취소 UseCase다. 주최자만 취소할 수 있고, 이미 종료/취소된 회의는 다시 취소할 수 없다(도메인에서 차단). 취소된 회의는 회의실 시간대 겹침 검사에서
 * 제외되므로, 취소 즉시 그 시간대를 다른 회의가 예약할 수 있다.
 */
@Service
public class CancelMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public CancelMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider,
            DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.meetingCalendarSyncPortProvider = meetingCalendarSyncPortProvider;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    @Transactional
    public MeetingResult execute(UUID meetingId, UUID requesterId) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.isHostedBy(requesterId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의 주최자만 취소할 수 있습니다.");
        }

        // 취소는 참석자 행을 건드리지 않지만, 발송 후 흐름과 무관하게 현재 참석자를 알림 대상으로 확보한다.
        List<MeetingAttendee> attendees =
                meetingAttendeeRepositoryPort.findByMeetingId(meeting.id());

        Meeting cancelled = meetingRepositoryPort.save(meeting.cancel());
        // 회의가 취소되면 개인 워크스페이스에 투영된 MEETING source 일정도 제거한다.
        // ifAvailable: MeetingCalendarSyncPort 구현체(개인 워크스페이스 모듈)가 아직 없으면 조용히 건너뛴다 → 회의 기능엔 영향 없음.
        meetingCalendarSyncPortProvider.ifAvailable(
                calendarSync -> calendarSync.removeMeetingEvents(cancelled.id()));
        notifyMeetingCancelled(cancelled, attendees, requesterId);
        return MeetingResult.of(cancelled);
    }

    /**
     * 회의 취소를 참석자에게 알린다. 취소를 실행한 주최자 본인은 제외한다.
     *
     * <p>알림은 {@link DispatchNotificationUseCase}가 현재 트랜잭션에 함께 저장하고 커밋 후 SSE로 전달한다(취소가 롤백되면 알림도 함께 롤백).
     */
    private void notifyMeetingCancelled(
            Meeting meeting, List<MeetingAttendee> attendees, UUID actorUserId) {
        for (MeetingAttendee attendee : attendees) {
            if (attendee.userId().equals(actorUserId)) {
                continue;
            }
            dispatchNotificationUseCase.execute(
                    new DispatchNotificationCommand(
                            attendee.userId(),
                            NotificationType.MEETING_CANCELLED.name(),
                            "회의 취소",
                            "\"" + meeting.title() + "\" 회의가 취소되었습니다.",
                            NotificationResourceType.MEETING.name(),
                            meeting.id()));
        }
    }
}
