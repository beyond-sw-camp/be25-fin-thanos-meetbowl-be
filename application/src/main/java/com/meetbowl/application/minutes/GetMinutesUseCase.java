package com.meetbowl.application.minutes;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 회의별 회의록 상세를 조회한다. */
@Service
public class GetMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MeetingAttendeeRepositoryPort attendeeRepositoryPort;
    private final MinutesMeetingMetadataAssembler metadataAssembler;

    public GetMinutesUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MeetingAttendeeRepositoryPort attendeeRepositoryPort,
            MinutesMeetingMetadataAssembler metadataAssembler) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.attendeeRepositoryPort = attendeeRepositoryPort;
        this.metadataAssembler = metadataAssembler;
    }

    @Transactional(readOnly = true)
    public MinutesResult get(
            UUID meetingId, UUID actorUserId, UUID actorOrganizationId, boolean isAdmin) {
        Minutes minutes =
                minutesRepositoryPort
                        .findByMeetingId(meetingId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
        MinutesAccessValidator.ensureSameOrganization(minutes, actorOrganizationId);
        if (isAdmin) {
            return MinutesResult.from(
                    minutes,
                    metadataAssembler.assemble(
                            minutes.meetingId(),
                            minutes.organizationId(),
                            minutes.reviewerUserId()));
        }
        if (!isMeetingParticipant(minutes.meetingId(), actorUserId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의 참석자만 회의록을 조회할 수 있습니다.");
        }
        MinutesAccessValidator.ensureReadable(minutes, actorUserId, actorOrganizationId);
        return MinutesResult.from(
                minutes,
                metadataAssembler.assemble(
                        minutes.meetingId(), minutes.organizationId(), minutes.reviewerUserId()));
    }

    private boolean isMeetingParticipant(UUID meetingId, UUID actorUserId) {
        return attendeeRepositoryPort.findByMeetingId(meetingId).stream()
                .anyMatch(attendee -> attendee.userId().equals(actorUserId));
    }
}
