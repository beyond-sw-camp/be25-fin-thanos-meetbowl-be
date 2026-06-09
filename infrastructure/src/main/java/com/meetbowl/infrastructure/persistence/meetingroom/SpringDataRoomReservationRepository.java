package com.meetbowl.infrastructure.persistence.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.meetingroom.ReservationStatus;

//
/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataRoomReservationRepository
        extends JpaRepository<RoomReservationEntity, UUID> {

    List<RoomReservationEntity> findByReservedByUserId(UUID reservedByUserId);

    boolean existsByMeetingRoomIdAndStatus(UUID meetingRoomId, ReservationStatus status);

    /**
     * 같은 회의실에서 [startedAt, endedAt) 구간과 겹치는 주어진 상태의 예약을 조회한다. 겹침 조건: existing.startedAt &lt;
     * endedAt AND existing.endedAt &gt; startedAt (경계 맞닿음은 제외).
     */
    @Query(
            "select r from RoomReservationEntity r "
                    + "where r.meetingRoomId = :meetingRoomId "
                    + "and r.status = :status "
                    + "and r.startedAt < :endedAt "
                    + "and r.endedAt > :startedAt")
    List<RoomReservationEntity> findOverlaps(
            @Param("meetingRoomId") UUID meetingRoomId,
            @Param("status") ReservationStatus status,
            @Param("startedAt") Instant startedAt,
            @Param("endedAt") Instant endedAt);

    /**
     * 전체 예약 현황 조회(F4). meetingRoomId/from/to가 null이면 해당 조건을 무시하고, 기간이 주어지면 [from, to)와 겹치는 예약만
     * 반환한다.
     */
    @Query(
            "select r from RoomReservationEntity r "
                    + "where (:meetingRoomId is null or r.meetingRoomId = :meetingRoomId) "
                    + "and (:from is null or r.endedAt > :from) "
                    + "and (:to is null or r.startedAt < :to)")
    Page<RoomReservationEntity> search(
            @Param("meetingRoomId") UUID meetingRoomId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /** 관리자 전체 예약 현황 조회(F16) - 회의실 미지정(전체). status/from/to가 null이면 해당 조건을 무시한다. */
    @Query(
            "select r from RoomReservationEntity r "
                    + "where (:status is null or r.status = :status) "
                    + "and (:from is null or r.endedAt > :from) "
                    + "and (:to is null or r.startedAt < :to)")
    Page<RoomReservationEntity> searchAdmin(
            @Param("status") ReservationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /** 관리자 전체 예약 현황 조회(F16) - 지정 회의실들로 제한. {@code meetingRoomIds}는 비어있지 않아야 한다. */
    @Query(
            "select r from RoomReservationEntity r "
                    + "where r.meetingRoomId in :meetingRoomIds "
                    + "and (:status is null or r.status = :status) "
                    + "and (:from is null or r.endedAt > :from) "
                    + "and (:to is null or r.startedAt < :to)")
    Page<RoomReservationEntity> searchAdminByRooms(
            @Param("meetingRoomIds") List<UUID> meetingRoomIds,
            @Param("status") ReservationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
