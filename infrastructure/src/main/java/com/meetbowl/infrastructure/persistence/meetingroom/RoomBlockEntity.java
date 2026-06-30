package com.meetbowl.infrastructure.persistence.meetingroom;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meetingroom.RoomBlock;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회의실 시간대 차단 JPA Entity다. {@code room_block} 테이블과 1:1로 매핑된다. 회의실은 raw UUID로 참조한다. 겹침 조회를 위해
 * {@code (room_id, start_at, end_at)} 복합 인덱스를 둔다.
 */
@Entity
@Table(
        name = "room_block",
        indexes = {
            @Index(name = "idx_room_block_room_time", columnList = "room_id, start_at, end_at")
        })
public class RoomBlockEntity extends BaseEntity {

    /** 차단 대상 회의실(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID roomId;

    /** 차단 시작 시각(포함, UTC). */
    @Column(nullable = false)
    private Instant startAt;

    /** 차단 종료 시각(제외, UTC). */
    @Column(nullable = false)
    private Instant endAt;

    /** 차단 사유(nullable). */
    @Column(length = 200)
    private String reason;

    protected RoomBlockEntity() {}

    private RoomBlockEntity(UUID roomId, Instant startAt, Instant endAt, String reason) {
        this.roomId = roomId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    static RoomBlockEntity from(RoomBlock roomBlock) {
        RoomBlockEntity entity =
                new RoomBlockEntity(
                        roomBlock.roomId(),
                        roomBlock.startAt(),
                        roomBlock.endAt(),
                        roomBlock.reason());
        entity.setId(roomBlock.id());
        return entity;
    }

    RoomBlock toDomain() {
        return RoomBlock.of(getId(), roomId, startAt, endAt, reason);
    }
}
