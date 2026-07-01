package com.meetbowl.infrastructure.persistence.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataRoomBlockRepository extends JpaRepository<RoomBlockEntity, UUID> {

    List<RoomBlockEntity> findByRoomId(UUID roomId);

    /** 한 회의실에서 {@code [from, to)}와 겹치는 차단(반개구간: start < to AND from < end). */
    @Query(
            "select b from RoomBlockEntity b"
                    + " where b.roomId = :roomId and b.startAt < :to and :from < b.endAt")
    List<RoomBlockEntity> findOverlapping(
            @Param("roomId") UUID roomId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** 여러 회의실에서 {@code [from, to)}와 겹치는 차단(보드/상태 화면용). */
    @Query(
            "select b from RoomBlockEntity b"
                    + " where b.roomId in :roomIds and b.startAt < :to and :from < b.endAt")
    List<RoomBlockEntity> findByRoomIdInAndRange(
            @Param("roomIds") List<UUID> roomIds,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
