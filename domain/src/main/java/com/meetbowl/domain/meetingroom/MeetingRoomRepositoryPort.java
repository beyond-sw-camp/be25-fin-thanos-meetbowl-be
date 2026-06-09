package com.meetbowl.domain.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의실 기준정보 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingRoomRepositoryPort {

    MeetingRoom save(MeetingRoom meetingRoom);

    Optional<MeetingRoom> findById(UUID id);

    /**
     * 예약 생성 시 회의실 단위 직렬화를 위해 회의실 행에 비관적 쓰기 잠금을 걸고 조회한다. 트랜잭션 내에서 호출해 겹침 검사 ~ 예약 저장을 경합 없이 수행하기 위한
     * 계약이다.
     */
    Optional<MeetingRoom> findByIdForUpdate(UUID id);

    List<MeetingRoom> findAll();

    List<MeetingRoom> findByBuildingId(UUID buildingId);

    void deleteById(UUID id);
}
