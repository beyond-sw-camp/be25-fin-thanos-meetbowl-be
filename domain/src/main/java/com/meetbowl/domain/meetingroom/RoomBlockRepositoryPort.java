package com.meetbowl.domain.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의실 시간대 차단 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface RoomBlockRepositoryPort {

    RoomBlock save(RoomBlock roomBlock);

    Optional<RoomBlock> findById(UUID id);

    /** 한 회의실의 차단 목록(관리자 조회·삭제용). */
    List<RoomBlock> findByRoomId(UUID roomId);

    /**
     * 한 회의실에서 주어진 구간 {@code [from, to)}와 겹치는 차단을 조회한다. 예약 생성/수정 시 가드가 차단 충돌을 검사하기 위해 사용한다. 회의실 단위 직렬화는
     * 호출자가 회의실 행에 건 비관적 잠금({@link MeetingRoomRepositoryPort#findByIdForUpdate})으로 보장하므로, 이 조회는 별도 잠금 없이
     * 같은 트랜잭션 안에서 읽는다.
     */
    List<RoomBlock> findOverlapping(UUID roomId, Instant from, Instant to);

    /**
     * 여러 회의실에 대해 주어진 구간 {@code [from, to)}와 겹치는 차단을 한 번에 조회한다. 예약 보드/상태 화면에서 날짜별 차단 구간을 회의실별로 그려주기 위해
     * 사용한다.
     */
    List<RoomBlock> findByRoomIdInAndRange(List<UUID> roomIds, Instant from, Instant to);

    void deleteById(UUID id);
}
