package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeConflict;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 생성·수정에서 공통으로 사용하는 참석자 시간 겹침 검증 컴포넌트다. 회의실 겹침 가드({@link MeetingRoomReservationGuard})의 참석자
 * 버전으로, 주최자를 포함한 참석자들이 같은 시간대의 다른 활성 회의(SCHEDULED/IN_PROGRESS)에 이미 잡혀 있으면 저장을 막는다.
 *
 * <p>회의실 가드와 달리 잠글 단일 행(사용자는 전역 엔티티)이 없어 비관적 잠금은 걸지 않는다(best-effort). 실시간 검사를 우회한 저장을 막는 최종 방어선이며,
 * 두 회의가 같은 사용자를 동시에 예약하는 극단적 경합은 허용 범위로 둔다. 수정 시에는 자기 회의를 {@code excludeMeetingId}로 제외한다(생성 시
 * null).
 */
@Component
public class MeetingAttendeeOverlapGuard {

    private final MeetingRepositoryPort meetingRepositoryPort;

    public MeetingAttendeeOverlapGuard(MeetingRepositoryPort meetingRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
    }

    /**
     * 참석자들이 [{@code scheduledAt}, {@code scheduledEndAt})에 다른 활성 회의에 잡혀 있으면 {@link
     * ErrorCode#ATTENDEE_TIME_CONFLICT}로 실패한다. 겹치는 사용자가 없으면 통과한다. 겹친 사용자별 상세는 실시간 검사 API가 제공하므로
     * 여기서는 차단만 한다.
     *
     * @param excludeMeetingId 겹침 검사에서 제외할 회의(수정 시 자기 자신). 생성 시에는 {@code null}.
     */
    public void verifyNoOverlap(
            Collection<UUID> userIds,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID excludeMeetingId) {
        List<AttendeeConflict> conflicts =
                meetingRepositoryPort.findActiveByAttendees(
                        userIds, scheduledAt, scheduledEndAt, excludeMeetingId);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTENDEE_TIME_CONFLICT);
        }
    }
}
