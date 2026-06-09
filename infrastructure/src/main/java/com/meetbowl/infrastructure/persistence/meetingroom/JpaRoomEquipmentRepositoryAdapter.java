package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meetingroom.RoomEquipment;
import com.meetbowl.domain.meetingroom.RoomEquipmentRepositoryPort;

/** RoomEquipment domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaRoomEquipmentRepositoryAdapter implements RoomEquipmentRepositoryPort {

    private final SpringDataRoomEquipmentRepository springDataRoomEquipmentRepository;

    public JpaRoomEquipmentRepositoryAdapter(
            SpringDataRoomEquipmentRepository springDataRoomEquipmentRepository) {
        this.springDataRoomEquipmentRepository = springDataRoomEquipmentRepository;
    }

    @Override
    public RoomEquipment save(RoomEquipment equipment) {
        return springDataRoomEquipmentRepository
                .save(RoomEquipmentEntity.from(equipment))
                .toDomain();
    }

    @Override
    public List<RoomEquipment> saveAll(List<RoomEquipment> equipments) {
        List<RoomEquipmentEntity> entities =
                equipments.stream().map(RoomEquipmentEntity::from).toList();
        return springDataRoomEquipmentRepository.saveAll(entities).stream()
                .map(RoomEquipmentEntity::toDomain)
                .toList();
    }

    @Override
    public List<RoomEquipment> findByMeetingRoomId(UUID meetingRoomId) {
        return springDataRoomEquipmentRepository.findByMeetingRoomId(meetingRoomId).stream()
                .map(RoomEquipmentEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByMeetingRoomId(UUID meetingRoomId) {
        springDataRoomEquipmentRepository.deleteByMeetingRoomId(meetingRoomId);
    }
}
