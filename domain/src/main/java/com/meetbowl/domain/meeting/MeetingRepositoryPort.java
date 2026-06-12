package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingRepositoryPort {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(UUID id);

    /** 내가 주최한 회의 조회용. */
    List<Meeting> findByHostUserId(UUID hostUserId);

    /**
     * 같은 회의실에서 주어진 예정 시간대 [{@code scheduledStartAt}, {@code scheduledEndAt})와 겹치는 활성 회의를 조회한다. 활성은
     * 상태가 SCHEDULED 또는 IN_PROGRESS인 회의를 말하며, 취소/종료된 회의는 제외한다. 회의실 중복 예약 방지에 사용한다.
     *
     * <p>겹침 판정은 {@code 기존.scheduledAt < scheduledEndAt AND 기존.scheduledEndAt > scheduledStartAt}이다.
     * 경계가 맞닿는 경우(이전 종료 == 다음 시작)는 겹침으로 보지 않는다. 호출 전 회의실 행에 비관적 잠금을 걸어 검사~저장 사이 경합을 막아야 한다.
     */
    List<Meeting> findActiveRoomOverlaps(
            UUID meetingRoomId, Instant scheduledStartAt, Instant scheduledEndAt);

    /**
     * 여러 회의실에 대해 주어진 시간대 [{@code from}, {@code to})와 겹치는 활성 회의(SCHEDULED/IN_PROGRESS)를 한 번에 조회한다.
     * 회의실 현황(F3) 계산에 사용한다. {@code meetingRoomIds}가 비어 있으면 빈 목록을 반환한다.
     */
    List<Meeting> findActiveOverlapsInRooms(List<UUID> meetingRoomIds, Instant from, Instant to);

    void deleteById(UUID id);
}
