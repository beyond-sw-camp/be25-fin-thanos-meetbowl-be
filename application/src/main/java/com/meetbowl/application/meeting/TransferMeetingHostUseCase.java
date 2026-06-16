package com.meetbowl.application.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 진행 권한을 기존 호스트에서 다른 참석자로 넘긴다.
 *
 * <p>회의 생성자가 잠시 자리를 비우더라도 회의가 계속될 수 있어야 하므로, 회의 본체의 hostUserId와 참석자 역할을 함께 갱신한다.
 */
@Service
@Transactional
public class TransferMeetingHostUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;

    public TransferMeetingHostUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
    }

    public MeetingResult execute(TransferMeetingHostCommand command) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(command.meetingId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        UUID requesterUserId = command.requesterUserId();
        if (requesterUserId != null && !meeting.isHostedBy(requesterUserId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의 주최자만 관리자를 변경할 수 있습니다.");
        }
        if (command.newHostUserId() == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "새 회의 관리자는 필수입니다.");
        }

        List<MeetingAttendee> attendees = meetingAttendeeRepositoryPort.findByMeetingId(meeting.id());
        MeetingAttendee newHostAttendee =
                attendees.stream()
                        .filter(attendee -> attendee.userId().equals(command.newHostUserId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_INVALID_REQUEST,
                                                "회의에 참여한 사용자만 관리자로 지정할 수 있습니다."));

        if (meeting.hostUserId().equals(command.newHostUserId())) {
            return MeetingResult.of(meeting, attendees);
        }

        // 기존 호스트는 참여자로 되돌리고, 새 관리자는 HOST 역할로 승격한다.
        List<MeetingAttendee> updatedAttendees =
                attendees.stream()
                        .map(
                                attendee -> {
                                    if (attendee.userId().equals(meeting.hostUserId())) {
                                        return attendee.withRole(AttendeeRole.PARTICIPANT);
                                    }
                                    if (attendee.userId().equals(newHostAttendee.userId())) {
                                        return attendee.withRole(AttendeeRole.HOST);
                                    }
                                    return attendee;
                                })
                        .toList();

        meetingAttendeeRepositoryPort.saveAll(updatedAttendees);
        Meeting savedMeeting = meetingRepositoryPort.save(meeting.transferHost(command.newHostUserId()));
        return MeetingResult.of(savedMeeting, updatedAttendees);
    }
}
