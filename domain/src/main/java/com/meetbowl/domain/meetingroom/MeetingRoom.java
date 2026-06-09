package com.meetbowl.domain.meetingroom;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의실 기준정보 도메인 모델이다.
 *
 * <p>관리자가 이름·소속 건물·층·위치·수용 인원·사용 가능 여부를 등록/수정/삭제한다(FR-088). 보유 장비는 "여러 개"라 별도 모델({@link
 * RoomEquipment})이 {@code meetingRoomId}로 참조해 소유한다. 사용자는 {@code isAvailable=true}인 회의실을 예약할 수 있고,
 * {@code false}이면 신규 예약을 차단한다. 건물은 {@code buildingId} raw UUID로 참조한다.
 */
public class MeetingRoom {

    private final UUID id;

    /** 소속 건물(FK). */
    private final UUID buildingId;

    /** 회의실명(필수). */
    private final String name;

    /** 층(선택). */
    private final Integer floor;

    /** 위치 설명(선택, 예: 엘리베이터 우측). */
    private final String location;

    /** 수용 인원(필수, 1 이상). */
    private final int capacity;

    /** 사용 가능 여부. false면 신규 예약 차단. */
    private final boolean available;

    private MeetingRoom(
            UUID id,
            UUID buildingId,
            String name,
            Integer floor,
            String location,
            int capacity,
            boolean available) {
        this.id = id;
        this.buildingId = buildingId;
        this.name = name;
        this.floor = floor;
        this.location = location;
        this.capacity = capacity;
        this.available = available;
    }

    public static MeetingRoom create(
            UUID buildingId, String name, Integer floor, String location, int capacity) {
        return of(null, buildingId, name, floor, location, capacity, true);
    }

    public static MeetingRoom of(
            UUID id,
            UUID buildingId,
            String name,
            Integer floor,
            String location,
            int capacity,
            boolean available) {
        if (buildingId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "소속 건물은 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의실명은 필수입니다.");
        }
        if (capacity <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "수용 인원은 1명 이상이어야 합니다.");
        }
        return new MeetingRoom(id, buildingId, name, floor, location, capacity, available);
    }

    public MeetingRoom change(
            UUID newBuildingId,
            String newName,
            Integer newFloor,
            String newLocation,
            int newCapacity) {
        return of(id, newBuildingId, newName, newFloor, newLocation, newCapacity, available);
    }

    /** 사용 가능 여부 전환(FR-089 관리자 사용 제한 설정). */
    public MeetingRoom changeAvailability(boolean newAvailable) {
        return of(id, buildingId, name, floor, location, capacity, newAvailable);
    }

    public UUID id() {
        return id;
    }

    public UUID buildingId() {
        return buildingId;
    }

    public String name() {
        return name;
    }

    public Integer floor() {
        return floor;
    }

    public String location() {
        return location;
    }

    public int capacity() {
        return capacity;
    }

    public boolean isAvailable() {
        return available;
    }
}
