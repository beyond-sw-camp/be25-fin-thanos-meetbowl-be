package com.meetbowl.application.meeting;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

/**
 * LiveKit 회의 채널이 열려 실제로 회의가 시작됐음을 DB에 기록한다.
 *
 * <p>회의가 아직 예정 상태면 진행 상태로 전환하고, 이미 진행 중이면 멱등적으로 무시한다. 종료/취소된 회의는 다시 시작할 수 없다.
 */
@Service
@Transactional
public class StartMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final Clock clock;

    public StartMeetingUseCase(MeetingRepositoryPort meetingRepositoryPort, Clock clock) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.clock = clock;
    }

    public void execute(UUID meetingId) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 ID는 필수입니다.");
        }

        Optional<Meeting> meetingOptional = meetingRepositoryPort.findById(meetingId);
        if (meetingOptional.isEmpty()) {
            return;
        }

        Meeting meeting = meetingOptional.get();
        if (meeting.status() == MeetingStatus.IN_PROGRESS) {
            return;
        }
        if (meeting.status() == MeetingStatus.ENDED) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }
        if (meeting.status() == MeetingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "취소된 회의는 시작할 수 없습니다.");
        }

        Meeting startedMeeting = meeting.start(Instant.now(clock));
        meetingRepositoryPort.save(startedMeeting);
    }
}
