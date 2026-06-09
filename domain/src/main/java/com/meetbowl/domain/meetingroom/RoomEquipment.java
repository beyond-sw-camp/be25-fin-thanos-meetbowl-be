package com.meetbowl.domain.meetingroom;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의실 보유 장비 도메인 모델이다.
 *
 * <p>회의실(MeetingRoom)이 장비를 "여러 개" 보유하므로 별도 행으로 관리한다(FR-088). 장비 종류는 {@code equipmentType}(예:
 * BEAM_PROJECTOR)로 표현하고, 회의실은 {@code meetingRoomId} raw UUID로 참조한다.
 */
public class RoomEquipment {

    private final UUID id;

    /** 소속 회의실(FK). */
    private final UUID meetingRoomId;

    /** 장비 종류(예: BEAM_PROJECTOR, VIDEO_CONFERENCE). */
    private final String equipmentType;

    /** 보유 수량(1 이상). */
    private final int quantity;

    private RoomEquipment(UUID id, UUID meetingRoomId, String equipmentType, int quantity) {
        this.id = id;
        this.meetingRoomId = meetingRoomId;
        this.equipmentType = equipmentType;
        this.quantity = quantity;
    }

    public static RoomEquipment create(UUID meetingRoomId, String equipmentType, int quantity) {
        return of(null, meetingRoomId, equipmentType, quantity);
    }

    public static RoomEquipment of(
            UUID id, UUID meetingRoomId, String equipmentType, int quantity) {
        if (meetingRoomId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "소속 회의실은 필수입니다.");
        }
        if (equipmentType == null || equipmentType.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "장비 종류는 필수입니다.");
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "장비 수량은 1 이상이어야 합니다.");
        }
        return new RoomEquipment(id, meetingRoomId, equipmentType, quantity);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingRoomId() {
        return meetingRoomId;
    }

    public String equipmentType() {
        return equipmentType;
    }

    public int quantity() {
        return quantity;
    }
}
