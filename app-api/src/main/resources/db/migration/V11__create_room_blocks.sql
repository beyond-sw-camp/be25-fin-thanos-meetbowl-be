-- 회의실 시간대 차단(room_block). 관리자가 회의실의 특정 구간 [start_at, end_at)을 예약 불가로 막는다.
-- 회의실은 raw UUID로 참조한다(meeting_room.building_id와 동일 컨벤션, DB FK 없음).
create table room_block (
    id BINARY(16) not null,
    room_id BINARY(16) not null,
    start_at datetime(6) not null,
    end_at datetime(6) not null,
    reason varchar(200),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

-- 겹침 조회(room_id 한정 + 시간 구간) 가속용 복합 인덱스. RoomBlockEntity의 idx_room_block_room_time과 일치.
create index idx_room_block_room_time
   on room_block (room_id, start_at, end_at);
