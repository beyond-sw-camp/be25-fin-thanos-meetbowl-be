package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingOrganizationResolver;
import com.meetbowl.domain.meeting.MeetingRealtimeSessionStopper;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 종료 상태를 `meetbowl-be` DB에 먼저 확정하고, 그 후 후속 처리를 위한 `meeting.ended` 이벤트를 발행한다.
 *
 * <p>종료 자체는 시스템의 authoritative state 변경이므로 동기 트랜잭션으로 처리하고, 회의록 생성 같은 느린 후속 단계만 RabbitMQ로 분리한다.
 */
@Service
@Transactional
public class EndMeetingUseCase {

    private static final Logger log = Logger.getLogger(EndMeetingUseCase.class.getName());

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final MeetingOrganizationResolver meetingOrganizationResolver;
    private final MeetingEndedEventPublisher meetingEndedEventPublisher;
    private final MeetingGuestNameAllocator meetingGuestNameAllocator;
    private final MeetingRealtimeSessionStopper meetingRealtimeSessionStopper;

    public EndMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            MeetingOrganizationResolver meetingOrganizationResolver,
            MeetingEndedEventPublisher meetingEndedEventPublisher,
            MeetingGuestNameAllocator meetingGuestNameAllocator,
            MeetingRealtimeSessionStopper meetingRealtimeSessionStopper) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.meetingOrganizationResolver = meetingOrganizationResolver;
        this.meetingEndedEventPublisher = meetingEndedEventPublisher;
        this.meetingGuestNameAllocator = meetingGuestNameAllocator;
        this.meetingRealtimeSessionStopper = meetingRealtimeSessionStopper;
    }

    public EndMeetingResult execute(EndMeetingCommand command) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(command.meetingId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        /**
         * 종료 요청은 여러 경로에서 다시 들어올 수 있다.
         * - Host가 UI에서 "회의 종료" 버튼을 누른 경우
         * - STT/시스템이 내부 API로 종료를 알린 경우
         * - 장애 복구 중 같은 종료 요청이 재처리된 경우
         *
         * 이미 ENDED라면 상태 변경과 이벤트 재발행을 막고, 게스트 번호 카운터만 정리한다.
         */
        if (meeting.status() == com.meetbowl.domain.meeting.MeetingStatus.ENDED) {
            meetingGuestNameAllocator.reset(meeting.id());
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

        /**
         * 회의 종료 이후 AI 회의록 초안 생성, 후속 알림 같은 비동기 작업은
         * DB 상태가 ENDED로 확정된 뒤 `meeting.ended` 이벤트로만 시작한다.
         */
        UUID reviewerUserId =
                meetingAttendeeRepositoryPort.findByMeetingId(savedMeeting.id()).stream()
                        .filter(MeetingAttendee::reviewer)
                        .map(MeetingAttendee::userId)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.MINUTES_REVIEWER_REQUIRED,
                                                "회의록을 생성하려면 검토자를 지정해야 합니다."));
        UUID organizationId =
                meetingOrganizationResolver.resolveByHostUserId(savedMeeting.hostUserId());

        boolean meetingEndedEventPublished = false;
        try {
            meetingEndedEventPublisher.publishMeetingEnded(
                    savedMeeting.id(),
                    organizationId,
                    savedMeeting.hostUserId(),
                    reviewerUserId,
                    savedMeeting.title(),
                    savedMeeting.startedAt(),
                    savedMeeting.endedAt(),
                    resolveCorrelationId(command.correlationId()));
            meetingEndedEventPublished = true;
        } catch (RuntimeException exception) {
            log.warning(
                    "회의 종료 후 meeting.ended 이벤트 발행에 실패했습니다. DB 종료 상태는 유지합니다. meetingId="
                            + savedMeeting.id()
                            + ", message="
                            + exception.getMessage());
        }

        /**
         * STT 세션 종료 실패가 authoritative 종료 자체를 되돌리면 안 된다.
         *
         * 회의 DB 상태와 후속 RabbitMQ 이벤트는 이미 확정됐으므로,
         * 여기서는 best-effort로 STT 세션 정리만 시도하고 실패 시 경고 로그로 남긴다.
         */
        try {
            meetingRealtimeSessionStopper.stop(savedMeeting.id());
        } catch (BusinessException exception) {
            log.warning(
                    "회의 종료 후 STT 세션 정리에 실패했습니다. meetingId="
                            + savedMeeting.id()
                            + ", errorCode="
                            + exception.errorCode()
                            + ", message="
                            + exception.getMessage());
        }

        // 같은 회의의 게스트 순번은 회의가 끝나면 더 이상 유지할 이유가 없으므로 즉시 초기화한다.
        meetingGuestNameAllocator.reset(savedMeeting.id());

        return new EndMeetingResult(
                savedMeeting.id(),
                savedMeeting.status().name(),
                savedMeeting.startedAt(),
                savedMeeting.endedAt(),
                meetingEndedEventPublished);
    }

    private Instant resolveEndedAt(Instant endedAt) {
        return endedAt != null ? endedAt : Instant.now();
    }

    private UUID resolveCorrelationId(UUID correlationId) {
        return correlationId != null ? correlationId : UUID.randomUUID();
    }
}
