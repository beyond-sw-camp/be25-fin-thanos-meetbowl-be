package com.meetbowl.application.meeting;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 취소 UseCase다. 주최자만 취소할 수 있고, 이미 종료/취소된 회의는 다시 취소할 수 없다(도메인에서 차단). 취소된 회의는 회의실 시간대 겹침 검사에서
 * 제외되므로, 취소 즉시 그 시간대를 다른 회의가 예약할 수 있다.
 */
@Service
public class CancelMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider;

    public CancelMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingCalendarSyncPortProvider = meetingCalendarSyncPortProvider;
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

        Meeting cancelled = meetingRepositoryPort.save(meeting.cancel());
        // 회의가 취소되면 개인 워크스페이스에 투영된 MEETING source 일정도 제거한다.
        // ifAvailable: MeetingCalendarSyncPort 구현체(개인 워크스페이스 모듈)가 아직 없으면 조용히 건너뛴다 → 회의 기능엔 영향 없음.
        meetingCalendarSyncPortProvider.ifAvailable(
                calendarSync -> calendarSync.removeMeetingEvents(cancelled.id()));
        return MeetingResult.of(cancelled);
    }
}
