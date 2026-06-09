package com.meetbowl.domain.meetingroom;

import java.util.List;
import java.util.UUID;

/** 회의실 장비 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface RoomEquipmentRepositoryPort {

    RoomEquipment save(RoomEquipment equipment);

    List<RoomEquipment> saveAll(List<RoomEquipment> equipments);

    List<RoomEquipment> findByMeetingRoomId(UUID meetingRoomId);

    void deleteByMeetingRoomId(UUID meetingRoomId);
}
