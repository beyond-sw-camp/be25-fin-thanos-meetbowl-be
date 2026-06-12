package com.meetbowl.application.meeting;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 수정 UseCase다. 주최자만 수정할 수 있고, 종료/취소된 회의는 수정할 수 없다(도메인에서 차단).
 *
 * <p>회의실·시간이 바뀌면 생성과 동일하게 회의실 행 비관적 잠금 후 시간대 겹침을 재검사한다. 단, 수정 중인 회의 자신은 겹침 대상에서 제외한다(자기 자신과의 겹침을
 * 충돌로 보지 않기 위함).
 */
@Service
public class UpdateMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingRoomReservationGuard reservationGuard;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider;

    public UpdateMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingRoomReservationGuard reservationGuard,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.reservationGuard = reservationGuard;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.meetingCalendarSyncPortProvider = meetingCalendarSyncPortProvider;
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
        List<MeetingAttendee> attendees = meetingAttendeeRepositoryPort.findByMeetingId(saved.id());
        syncCalendar(saved, attendees);
        return MeetingResult.of(saved, attendees);
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
