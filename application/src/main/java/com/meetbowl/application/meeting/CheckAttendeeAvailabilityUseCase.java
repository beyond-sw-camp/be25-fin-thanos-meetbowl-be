package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 참석자 시간 겹침 실시간 검사 UseCase다. 프론트가 회의 생성/수정 폼에서 참석자를 추가하거나 시간을 바꿀 때 호출해, 선택된 사용자들이 해당 시간대에 이미 다른 활성
 * 회의(SCHEDULED/IN_PROGRESS)에 참석/주최로 잡혀 있는지 (사용자, 회의) 쌍으로 돌려준다.
 *
 * <p>저장을 변경하지 않는 조회 전용(읽기 트랜잭션)이며, 저장을 막는 최종 방어는 생성/수정 시 {@link MeetingAttendeeOverlapGuard}가 한다.
 */
@Service
public class CheckAttendeeAvailabilityUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;

    public CheckAttendeeAvailabilityUseCase(MeetingRepositoryPort meetingRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<AttendeeConflictResult> execute(
            Collection<UUID> userIds, Instant from, Instant to, UUID excludeMeetingId) {
        return meetingRepositoryPort
                .findActiveByAttendees(userIds, from, to, excludeMeetingId)
                .stream()
                .map(AttendeeConflictResult::of)
                .toList();
    }
}
