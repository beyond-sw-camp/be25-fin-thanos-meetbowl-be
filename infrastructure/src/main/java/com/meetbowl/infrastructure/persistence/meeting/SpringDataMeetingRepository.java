package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.meeting.MeetingStatus;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataMeetingRepository extends JpaRepository<MeetingEntity, UUID> {

    List<MeetingEntity> findByHostUserId(UUID hostUserId);

    /**
     * 같은 회의실에서 [{@code startAt}, {@code endAt}) 시간대와 겹치는 활성 회의를 조회한다. 겹침 조건은 {@code 기존.scheduledAt
     * < endAt AND 기존.scheduledEndAt > startAt}이라 경계가 맞닿는 경우는 제외된다. 활성 상태 집합은 호출 측에서 전달한다.
     */
    @Query(
            "select m from MeetingEntity m"
                    + " where m.meetingRoomId = :roomId"
                    + " and m.status in :activeStatuses"
                    + " and m.scheduledAt < :endAt"
                    + " and m.scheduledEndAt > :startAt")
    List<MeetingEntity> findActiveRoomOverlaps(
            @Param("roomId") UUID roomId,
            @Param("activeStatuses") List<MeetingStatus> activeStatuses,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);

    /** 여러 회의실의 [startAt, endAt) 겹침 활성 회의를 한 번에 조회한다(회의실 현황 F3). */
    @Query(
            "select m from MeetingEntity m"
                    + " where m.meetingRoomId in :roomIds"
                    + " and m.status in :activeStatuses"
                    + " and m.scheduledAt < :endAt"
                    + " and m.scheduledEndAt > :startAt")
    List<MeetingEntity> findActiveOverlapsInRooms(
            @Param("roomIds") List<UUID> roomIds,
            @Param("activeStatuses") List<MeetingStatus> activeStatuses,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);
}
