package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataMeetingRoomRepository extends JpaRepository<MeetingRoomEntity, UUID> {

    List<MeetingRoomEntity> findByBuildingId(UUID buildingId);

    /**
     * 회의실 행에 비관적 쓰기 잠금(SELECT ... FOR UPDATE)을 건다. 예약 생성 트랜잭션에서 같은 회의실에 대한 동시 요청을 직렬화해, 겹침 검사 ~ 예약
     * INSERT 사이의 경합을 막는다(중복 예약 방지 핵심).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MeetingRoomEntity m where m.id = :id")
    Optional<MeetingRoomEntity> findByIdForUpdate(@Param("id") UUID id);
}
