package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 종료 상태를 `meetbowl-be` DB에 먼저 확정하고, 그 후 후속 처리를 위한 `meeting.ended` 이벤트를 발행한다.
 *
 * <p>종료 자체는 시스템의 authoritative state 변경이므로 동기 트랜잭션으로 처리하고, 회의록 생성 같은 느린 후속 단계만 RabbitMQ로 분리한다.
 */
@Service
@Transactional
public class EndMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final MeetingEndedEventPublisher meetingEndedEventPublisher;

    public EndMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            MeetingEndedEventPublisher meetingEndedEventPublisher) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.meetingEndedEventPublisher = meetingEndedEventPublisher;
    }

    public EndMeetingResult execute(EndMeetingCommand command) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(command.meetingId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        if (meeting.status() == com.meetbowl.domain.meeting.MeetingStatus.ENDED) {
            return new EndMeetingResult(
                    meeting.id(),
                    meeting.status().name(),
                    meeting.startedAt(),
                    meeting.endedAt(),
                    false);
        }

        Meeting endedMeeting =
                meeting.completeFromExternalSession(resolveEndedAt(command.endedAt()));
        Meeting savedMeeting = meetingRepositoryPort.save(endedMeeting);

        UUID reviewerUserId =
                meetingAttendeeRepositoryPort.findByMeetingId(savedMeeting.id()).stream()
                        .filter(attendee -> attendee.role() == AttendeeRole.REVIEWER)
                        .map(attendee -> attendee.userId())
                        .findFirst()
                        .orElse(null);

        meetingEndedEventPublisher.publishMeetingEnded(
                savedMeeting.id(),
                savedMeeting.hostUserId(),
                reviewerUserId,
                savedMeeting.title(),
                savedMeeting.startedAt(),
                savedMeeting.endedAt(),
                resolveCorrelationId(command.correlationId()));

        return new EndMeetingResult(
                savedMeeting.id(),
                savedMeeting.status().name(),
                savedMeeting.startedAt(),
                savedMeeting.endedAt(),
                true);
    }

    private Instant resolveEndedAt(Instant endedAt) {
        return endedAt != null ? endedAt : Instant.now();
    }

    private UUID resolveCorrelationId(UUID correlationId) {
        return correlationId != null ? correlationId : UUID.randomUUID();
    }
}
