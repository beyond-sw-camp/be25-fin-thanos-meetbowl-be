package com.meetbowl.application.meeting;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
 * 회의 수정 UseCase다. 주최자만 수정할 수 있고, 종료/취소된 회의는 수정할 수 없다(도메인에서 차단).
 *
 * <p>회의실·시간이 바뀌면 생성과 동일하게 회의실 행 비관적 잠금 후 사용 가능 여부·시간대 겹침을 재검사한다. 단, 수정 중인 회의 자신은 겹침 대상에서 제외한다(자기
 * 자신과의 겹침을 충돌로 보지 않기 위함).
 *
 * <p>참석자·검토자도 생성과 동일 규칙(참석자 최소 1명, 검토자는 참석자 중 1명)으로 전체 교체한다({@link MeetingAttendeeWriter}). 참석자를 바꿔
 * 기존 검토자가 새 목록에서 빠지면 검토자 제약에서 걸린다.
 */
@Service
public class UpdateMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final MeetingRoomReservationGuard reservationGuard;
    private final MeetingAttendeeWriter meetingAttendeeWriter;
    private final ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public UpdateMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            MeetingRoomReservationGuard reservationGuard,
            MeetingAttendeeWriter meetingAttendeeWriter,
            ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider,
            DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.reservationGuard = reservationGuard;
        this.meetingAttendeeWriter = meetingAttendeeWriter;
        this.meetingCalendarSyncPortProvider = meetingCalendarSyncPortProvider;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    @Transactional
    public MeetingResult execute(UpdateMeetingCommand command) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(command.meetingId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.isHostedBy(command.requesterId())) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의 주최자만 수정할 수 있습니다.");
        }

        // 알림 대상은 "기존 참석자 ∪ 새 참석자"라, 참석자 교체 전에 기존 목록을 먼저 확보해 둔다.
        List<MeetingAttendee> previousAttendees =
                meetingAttendeeRepositoryPort.findByMeetingId(meeting.id());

        // 변경 검증(제목 필수, 예정 시작 < 예정 종료, 종료/취소 회의 수정 불가)은 도메인에서 수행된다.
        Meeting changed =
                meeting.change(
                        command.title(),
                        command.scheduledAt(),
                        command.scheduledEndAt(),
                        command.meetingRoomId(),
                        command.description());

        if (changed.meetingRoomId() != null) {
            // 회의실/시간 변경 시 재검증. 수정이므로 자기 자신은 겹침 대상에서 제외한다.
            reservationGuard.verifyAvailable(
                    changed.meetingRoomId(),
                    changed.scheduledAt(),
                    changed.scheduledEndAt(),
                    changed.id());
        }

        Meeting saved = meetingRepositoryPort.save(changed);
        // 참석자·검토자 전체 교체(생성과 동일 규칙으로 재검증). 주최자는 회의 호스트로 항상 재포함된다.
        List<MeetingAttendee> attendees =
                meetingAttendeeWriter.replace(
                        saved.id(),
                        saved.hostUserId(),
                        command.attendeeUserIds(),
                        command.reviewerUserId());
        syncCalendar(saved, attendees);
        notifyMeetingUpdated(saved, previousAttendees, attendees, command.requesterId());
        return MeetingResult.of(saved, attendees);
    }

    /**
     * 일정이 바뀌었음을 기존 참석자와 새 참석자(합집합)에게 알린다.
     *
     * <p>회의에서 빠진 사람도 변경 사실을 알아야 하므로 교체 전·후 목록을 합치고, 변경을 실행한 주최자 본인은 제외한다. 알림은 {@link
     * DispatchNotificationUseCase}가 현재 트랜잭션에 함께 저장하고 커밋 후 SSE로 전달한다(수정이 롤백되면 알림도 함께 롤백).
     */
    private void notifyMeetingUpdated(
            Meeting meeting,
            List<MeetingAttendee> previousAttendees,
            List<MeetingAttendee> currentAttendees,
            UUID actorUserId) {
        Set<UUID> recipientUserIds = new LinkedHashSet<>();
        previousAttendees.forEach(attendee -> recipientUserIds.add(attendee.userId()));
        currentAttendees.forEach(attendee -> recipientUserIds.add(attendee.userId()));
        recipientUserIds.remove(actorUserId);

        for (UUID recipientUserId : recipientUserIds) {
            dispatchNotificationUseCase.execute(
                    new DispatchNotificationCommand(
                            recipientUserId,
                            NotificationType.MEETING_UPDATED.name(),
                            "회의 일정 변경",
                            "\"" + meeting.title() + "\" 회의 일정이 변경되었습니다.",
                            NotificationResourceType.MEETING.name(),
                            meeting.id()));
        }
    }

    private void syncCalendar(Meeting meeting, List<MeetingAttendee> attendees) {
        // 회의 수정은 같은 meetingId로 다시 투영한다. 개인 워크스페이스 구현체가 있으면 기존 MEETING 일정을 갱신하고, 없으면 생성한다(멱등
        // upsert).
        // ifAvailable: 구현체(개인 워크스페이스 모듈)가 아직 없으면 조용히 건너뛴다 → 회의 기능엔 영향 없음.
        meetingCalendarSyncPortProvider.ifAvailable(
                calendarSync ->
                        calendarSync.syncFromMeeting(
                                new MeetingCalendarSyncCommand(
                                        meeting.id(),
                                        attendees.stream().map(MeetingAttendee::userId).toList(),
                                        meeting.title(),
                                        null,
                                        meeting.scheduledAt(),
                                        meeting.scheduledEndAt())));
    }
}
