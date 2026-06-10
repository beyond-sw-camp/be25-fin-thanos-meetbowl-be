package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;

/** MeetingRoom domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaMeetingRoomRepositoryAdapter implements MeetingRoomRepositoryPort {

    private final SpringDataMeetingRoomRepository springDataMeetingRoomRepository;

    public JpaMeetingRoomRepositoryAdapter(
            SpringDataMeetingRoomRepository springDataMeetingRoomRepository) {
        this.springDataMeetingRoomRepository = springDataMeetingRoomRepository;
    }

    @Override
    public MeetingRoom save(MeetingRoom meetingRoom) {
        return springDataMeetingRoomRepository.save(MeetingRoomEntity.from(meetingRoom)).toDomain();
    }

    @Override
    public Optional<MeetingRoom> findById(UUID id) {
        return springDataMeetingRoomRepository.findById(id).map(MeetingRoomEntity::toDomain);
    }

    @Override
    public Optional<MeetingRoom> findByIdForUpdate(UUID id) {
        return springDataMeetingRoomRepository
                .findByIdForUpdate(id)
                .map(MeetingRoomEntity::toDomain);
    }

    @Override
    public List<MeetingRoom> findAll() {
        return springDataMeetingRoomRepository.findAll().stream()
                .map(MeetingRoomEntity::toDomain)
                .toList();
    }

    @Override
    public List<MeetingRoom> findByBuildingId(UUID buildingId) {
        return springDataMeetingRoomRepository.findByBuildingId(buildingId).stream()
                .map(MeetingRoomEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataMeetingRoomRepository.deleteById(id);
    }
}
