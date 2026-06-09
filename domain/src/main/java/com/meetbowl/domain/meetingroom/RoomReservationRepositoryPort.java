package com.meetbowl.domain.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.meetbowl.domain.common.Paged;

/** 회의실 예약 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface RoomReservationRepositoryPort {

    RoomReservation save(RoomReservation reservation);

    Optional<RoomReservation> findById(UUID id);

    /**
     * 전체 예약 현황을 필터/페이지로 조회한다(F4). 인자가 null이면 해당 조건은 무시한다. 기간 필터는 [from, to)와 겹치는 예약을 반환한다. {@code
     * page}는 1부터 시작한다.
     */
    Paged<RoomReservation> search(UUID meetingRoomId, Instant from, Instant to, int page, int size);

    /**
     * 관리자 전체 예약 현황 조회다(F16). {@code meetingRoomIds}가 null이면 전체 회의실, 비어있지 않으면 그 회의실들로 제한한다. {@code
     * status}가 null이면 모든 상태를 포함한다. 기간 필터는 [from, to)와 겹치는 예약을 반환하며 인자가 null이면 무시한다. {@code page}는
     * 1부터 시작한다.
     */
    Paged<RoomReservation> searchReservations(
            List<UUID> meetingRoomIds,
            ReservationStatus status,
            Instant from,
            Instant to,
            int page,
            int size);

    /** 회의실에 활성(RESERVED) 예약이 하나라도 있는지 확인한다. 관리자 회의실 삭제/제한 차단에 사용한다(F2). */
    boolean hasActiveReservations(UUID meetingRoomId);

    /** 내 회의실 예약 현황 조회용. (FR-022) */
    List<RoomReservation> findByReservedByUserId(UUID userId);

    /**
     * 같은 회의실에서 주어진 시간대와 겹치는 활성(RESERVED) 예약을 조회한다. 중복 예약 방지(FR-021)와 현황 조회(FR-020)에 사용한다. 경계가 맞닿는
     * 경우(이전 종료 == 다음 시작)는 겹침으로 보지 않는다.
     */
    List<RoomReservation> findActiveOverlaps(
            UUID meetingRoomId, Instant startedAt, Instant endedAt);

    void deleteById(UUID id);
}
