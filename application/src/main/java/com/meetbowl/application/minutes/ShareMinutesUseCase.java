package com.meetbowl.application.minutes;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.mail.SendMailCommand;
import com.meetbowl.application.mail.SendMailUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

/** 승인된 회의록을 회의에 참여하지 않은 사용자에게 추가 공유한다. */
@Service
public class ShareMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MeetingAttendeeRepositoryPort attendeeRepositoryPort;
    private final SendMailUseCase sendMailUseCase;
    private final MinutesMeetingMetadataAssembler metadataAssembler;
    private final Clock clock;

    public ShareMinutesUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MeetingAttendeeRepositoryPort attendeeRepositoryPort,
            SendMailUseCase sendMailUseCase,
            MinutesMeetingMetadataAssembler metadataAssembler,
            Clock clock) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.attendeeRepositoryPort = attendeeRepositoryPort;
        this.sendMailUseCase = sendMailUseCase;
        this.metadataAssembler = metadataAssembler;
        this.clock = clock;
    }

    @Transactional
    public MinutesResult execute(ShareMinutesCommand command) {
        Minutes minutes =
                minutesRepositoryPort
                        .findByMeetingId(command.meetingId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
        MinutesAccessValidator.ensureSameOrganization(minutes, command.actorOrganizationId());
        if (minutes.status() != MinutesStatus.APPROVED
                && minutes.status() != MinutesStatus.SHARED) {
            throw new BusinessException(ErrorCode.MINUTES_REVIEW_REQUIRED, "승인 전 회의록은 공유할 수 없습니다.");
        }
        ensureRecipientsAreNotAttendees(minutes.meetingId(), command.recipientUserIds());

        sendMailUseCase.execute(
                new SendMailCommand(
                        minutes.organizationId(),
                        command.actorUserId(),
                        List.copyOf(command.recipientUserIds()),
                        command.subject(),
                        command.body(),
                        "MINUTES_SHARE",
                        "MEETING_MINUTES",
                        minutes.id(),
                        command.idempotencyKey()));

        Minutes shared = minutesRepositoryPort.save(minutes.markShared(Instant.now(clock)));
        return MinutesResult.from(
                shared,
                metadataAssembler.assemble(
                        shared.meetingId(), shared.organizationId(), shared.reviewerUserId()));
    }

    private void ensureRecipientsAreNotAttendees(UUID meetingId, List<UUID> recipientUserIds) {
        var attendeeUserIds =
                new HashSet<>(
                        attendeeRepositoryPort.findByMeetingId(meetingId).stream()
                                .map(MeetingAttendee::userId)
                                .toList());
        boolean hasAttendee = recipientUserIds.stream().anyMatch(attendeeUserIds::contains);
        if (hasAttendee) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의 참여자는 승인 시 자동 공유되므로 수동 공유 대상에서 제외해야 합니다.");
        }
    }
}
