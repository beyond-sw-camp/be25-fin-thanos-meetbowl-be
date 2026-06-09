package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회의실 JPA Entity다. {@code meeting_room} 테이블과 1:1로 매핑된다. 건물은 raw UUID로 참조하고, 보유 장비는 별도 테이블({@link
 * RoomEquipmentEntity})이 소유한다.
 */
@Entity
@Table(
        name = "meeting_room",
        indexes = {@Index(name = "idx_meeting_room_building", columnList = "building_id")})
public class MeetingRoomEntity extends BaseEntity {

    /** 소속 건물(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID buildingId;

    /** 회의실명. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 층(nullable). */
    @Column private Integer floor;

    /** 위치 설명(nullable). */
    @Column(length = 200)
    private String location;

    /** 수용 인원. */
    @Column(nullable = false)
    private int capacity;

    /** 사용 가능 여부. false면 신규 예약 차단. */
    @Column(nullable = false)
    private boolean available;

    protected MeetingRoomEntity() {}

    private MeetingRoomEntity(
            UUID buildingId,
            String name,
            Integer floor,
            String location,
            int capacity,
            boolean available) {
        this.buildingId = buildingId;
        this.name = name;
        this.floor = floor;
        this.location = location;
        this.capacity = capacity;
        this.available = available;
    }

    static MeetingRoomEntity from(MeetingRoom meetingRoom) {
        MeetingRoomEntity entity =
                new MeetingRoomEntity(
                        meetingRoom.buildingId(),
                        meetingRoom.name(),
                        meetingRoom.floor(),
                        meetingRoom.location(),
                        meetingRoom.capacity(),
                        meetingRoom.isAvailable());
        entity.setId(meetingRoom.id());
        return entity;
    }

    MeetingRoom toDomain() {
        return MeetingRoom.of(getId(), buildingId, name, floor, location, capacity, available);
    }
}
