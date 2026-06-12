package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;

/**
 * 회의 생성·수정에서 공통으로 사용하는 회의실 예약 가능 검증 컴포넌트다.
 *
 * <p>회의실 행에 비관적 쓰기 잠금을 걸어(존재 검증 포함) 같은 회의실에 대한 동시 요청을 직렬화하고, 잠금이 걸린 상태에서 활성 회의(SCHEDULED/IN_PROGRESS)와
 * 시간대가 겹치는지 검사한다. 호출자(생성/수정 UseCase)의 트랜잭션 안에서 실행되므로 잠금이 검사~저장 구간의 경합을 막는다.
 *
 * <p>생성과 수정의 검증 로직이 동일하여 한 곳으로 공통화했다. 엔드포인트(회의 생성/수정)는 분리돼 있고, 내부 검증만 재사용한다. 수정 시에는 자기 자신과의 겹침을
 * 충돌로 보면 안 되므로 {@code excludeMeetingId}로 제외한다(생성 시에는 null).
 */
@Component
public class MeetingRoomReservationGuard {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;

    public MeetingRoomReservationGuard(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingRoomRepositoryPort meetingRoomRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
    }

    /**
     * 회의실 행을 잠근 뒤(존재 검증 포함) 시간대 겹침을 검사한다. 겹치는 활성 회의가 있으면 {@link
     * ErrorCode#MEETING_ROOM_ALREADY_RESERVED}로 실패한다.
     *
     * @param excludeMeetingId 겹침 검사에서 제외할 회의(수정 시 자기 자신). 생성 시에는 {@code null}을 넘긴다.
     */
    public void verifyAvailable(
            UUID meetingRoomId,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID excludeMeetingId) {
        meetingRoomRepositoryPort
                .findByIdForUpdate(meetingRoomId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "회의실을 찾을 수 없습니다."));

        boolean overlaps =
                meetingRepositoryPort
                        .findActiveRoomOverlaps(meetingRoomId, scheduledAt, scheduledEndAt)
                        .stream()
                        .anyMatch(
                                other ->
                                        excludeMeetingId == null
                                                || !other.id().equals(excludeMeetingId));
        if (overlaps) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_ALREADY_RESERVED);
        }
    }
}
