package com.meetbowl.application.meetingroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;

class MeetingRoomAdminUseCaseTest {

    @Test
    void createPersistsRoom() {
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        UUID buildingId = buildingRepository.addBuilding();
        FakeMeetingRoomRepositoryPort roomRepository = new FakeMeetingRoomRepositoryPort();

        MeetingRoomAdminUseCase useCase =
                new MeetingRoomAdminUseCase(roomRepository, buildingRepository);

        MeetingRoomResult result =
                useCase.create(
                        new CreateMeetingRoomCommand(
                                buildingId, "대회의실 1", 3, "엘리베이터 우측", 12, true));

        assertEquals("대회의실 1", result.name());
        assertTrue(result.available());
        assertEquals(1, roomRepository.rooms.size());
    }

    @Test
    void createRejectsUnknownBuilding() {
        MeetingRoomAdminUseCase useCase =
                new MeetingRoomAdminUseCase(
                        new FakeMeetingRoomRepositoryPort(), new FakeBuildingRepositoryPort());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new CreateMeetingRoomCommand(
                                                UUID.randomUUID(), "회의실", null, null, 10, true)));
        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void deleteRemovesRoom() {
        FakeMeetingRoomRepositoryPort roomRepository = new FakeMeetingRoomRepositoryPort();
        UUID roomId = roomRepository.addRoom();

        MeetingRoomAdminUseCase useCase =
                new MeetingRoomAdminUseCase(roomRepository, new FakeBuildingRepositoryPort());

        useCase.delete(roomId);

        assertTrue(roomRepository.rooms.isEmpty());
    }

    @Test
    void deleteRejectsUnknownRoom() {
        MeetingRoomAdminUseCase useCase =
                new MeetingRoomAdminUseCase(
                        new FakeMeetingRoomRepositoryPort(), new FakeBuildingRepositoryPort());

        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> useCase.delete(UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void changeAvailabilityUpdatesFlag() {
        FakeMeetingRoomRepositoryPort roomRepository = new FakeMeetingRoomRepositoryPort();
        UUID roomId = roomRepository.addRoom();

        MeetingRoomAdminUseCase useCase =
                new MeetingRoomAdminUseCase(roomRepository, new FakeBuildingRepositoryPort());

        MeetingRoomResult result = useCase.changeAvailability(roomId, false);

        assertFalse(result.available());
    }

    private static class FakeMeetingRoomRepositoryPort implements MeetingRoomRepositoryPort {

        private final Map<UUID, MeetingRoom> rooms = new HashMap<>();

        UUID addRoom() {
            UUID id = UUID.randomUUID();
            rooms.put(id, MeetingRoom.of(id, UUID.randomUUID(), "회의실", 1, "위치", 10, true));
            return id;
        }

        @Override
        public MeetingRoom save(MeetingRoom meetingRoom) {
            UUID id = meetingRoom.id() == null ? UUID.randomUUID() : meetingRoom.id();
            MeetingRoom stored =
                    MeetingRoom.of(
                            id,
                            meetingRoom.buildingId(),
                            meetingRoom.name(),
                            meetingRoom.floor(),
                            meetingRoom.location(),
                            meetingRoom.capacity(),
                            meetingRoom.isAvailable());
            rooms.put(id, stored);
            return stored;
        }

        @Override
        public Optional<MeetingRoom> findById(UUID id) {
            return Optional.ofNullable(rooms.get(id));
        }

        @Override
        public Optional<MeetingRoom> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public List<MeetingRoom> findAll() {
            return List.copyOf(rooms.values());
        }

        @Override
        public List<MeetingRoom> findByBuildingId(UUID buildingId) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {
            rooms.remove(id);
        }
    }

    private static class FakeBuildingRepositoryPort implements BuildingRepositoryPort {

        private final Map<UUID, Building> buildings = new HashMap<>();

        UUID addBuilding() {
            UUID id = UUID.randomUUID();
            buildings.put(id, Building.of(id, UUID.randomUUID(), "A동"));
            return id;
        }

        @Override
        public Building save(Building building) {
            return building;
        }

        @Override
        public Optional<Building> findById(UUID id) {
            return Optional.ofNullable(buildings.get(id));
        }

        @Override
        public List<Building> findAll() {
            return List.copyOf(buildings.values());
        }

        @Override
        public List<Building> findBySiteId(UUID siteId) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {
            buildings.remove(id);
        }
    }
}