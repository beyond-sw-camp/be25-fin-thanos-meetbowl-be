package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meetingroom.RoomEquipment;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 회의실 장비 JPA Entity다. {@code room_equipment} 테이블과 1:1로 매핑된다. 회의실은 raw UUID로 참조한다. */
@Entity
@Table(
        name = "room_equipment",
        indexes = {@Index(name = "idx_room_equipment_room", columnList = "meeting_room_id")})
public class RoomEquipmentEntity extends BaseEntity {

    /** 소속 회의실(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingRoomId;

    /** 장비 종류(예: BEAM_PROJECTOR). */
    @Column(nullable = false, length = 50)
    private String equipmentType;

    /** 보유 수량. */
    @Column(nullable = false)
    private int quantity;

    protected RoomEquipmentEntity() {}

    private RoomEquipmentEntity(UUID meetingRoomId, String equipmentType, int quantity) {
        this.meetingRoomId = meetingRoomId;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
    }

    static RoomEquipmentEntity from(RoomEquipment equipment) {
        RoomEquipmentEntity entity =
                new RoomEquipmentEntity(
                        equipment.meetingRoomId(), equipment.equipmentType(), equipment.quantity());
        entity.setId(equipment.id());
        return entity;
    }

    RoomEquipment toDomain() {
        return RoomEquipment.of(getId(), meetingRoomId, equipmentType, quantity);
    }
}
