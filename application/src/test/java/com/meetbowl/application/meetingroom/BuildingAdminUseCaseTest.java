package com.meetbowl.application.meetingroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

class BuildingAdminUseCaseTest {

    @Test
    void createValidatesSite() {
        BuildingAdminUseCase useCase =
                new BuildingAdminUseCase(
                        new FakeBuildingRepositoryPort(),
                        new FakeSiteRepositoryPort(),
                        new FakeMeetingRoomRepositoryPort());

        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> useCase.create(UUID.randomUUID(), "A동"));
        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void createSucceedsWhenSiteExists() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        UUID siteId = siteRepository.addSite();
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();

        BuildingAdminUseCase useCase =
                new BuildingAdminUseCase(
                        buildingRepository, siteRepository, new FakeMeetingRoomRepositoryPort());

        BuildingResult result = useCase.create(siteId, "A동");

        assertEquals(siteId, result.siteId());
        assertEquals("A동", result.name());
    }

    @Test
    void deleteBlockedWhenRoomsExist() {
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        UUID buildingId = buildingRepository.addBuilding();
        FakeMeetingRoomRepositoryPort roomRepository = new FakeMeetingRoomRepositoryPort();
        roomRepository.addRoom(buildingId);

        BuildingAdminUseCase useCase =
                new BuildingAdminUseCase(
                        buildingRepository, new FakeSiteRepositoryPort(), roomRepository);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.delete(buildingId));
        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void deleteSucceedsWhenNoRooms() {
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        UUID buildingId = buildingRepository.addBuilding();

        BuildingAdminUseCase useCase =
                new BuildingAdminUseCase(
                        buildingRepository,
                        new FakeSiteRepositoryPort(),
                        new FakeMeetingRoomRepositoryPort());

        useCase.delete(buildingId);

        assertTrue(buildingRepository.buildings.isEmpty());
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
            UUID id = building.id() == null ? UUID.randomUUID() : building.id();
            Building stored = Building.of(id, building.siteId(), building.name());
            buildings.put(id, stored);
            return stored;
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

    private static class FakeSiteRepositoryPort implements SiteRepositoryPort {

        private final Map<UUID, Site> sites = new HashMap<>();

        UUID addSite() {
            UUID id = UUID.randomUUID();
            sites.put(id, Site.of(id, UUID.randomUUID(), "사이트", null));
            return id;
        }

        @Override
        public Site save(Site site) {
            return site;
        }

        @Override
        public Optional<Site> findById(UUID id) {
            return Optional.ofNullable(sites.get(id));
        }

        @Override
        public List<Site> findAll() {
            return List.copyOf(sites.values());
        }

        @Override
        public void deleteById(UUID id) {
            sites.remove(id);
        }
    }

    private static class FakeMeetingRoomRepositoryPort implements MeetingRoomRepositoryPort {

        private final List<MeetingRoom> rooms = new ArrayList<>();

        void addRoom(UUID buildingId) {
            rooms.add(MeetingRoom.of(UUID.randomUUID(), buildingId, "회의실", 1, "위치", 10, true));
        }

        @Override
        public MeetingRoom save(MeetingRoom meetingRoom) {
            return meetingRoom;
        }

        @Override
        public Optional<MeetingRoom> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<MeetingRoom> findByIdForUpdate(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<MeetingRoom> findAll() {
            return List.copyOf(rooms);
        }

        @Override
        public List<MeetingRoom> findByBuildingId(UUID buildingId) {
            return rooms.stream().filter(r -> r.buildingId().equals(buildingId)).toList();
        }

        @Override
        public void deleteById(UUID id) {}
    }
}
