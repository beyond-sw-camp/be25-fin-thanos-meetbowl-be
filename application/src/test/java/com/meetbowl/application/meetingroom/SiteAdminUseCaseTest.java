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
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

class SiteAdminUseCaseTest {

    @Test
    void createReturnsSite() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        SiteAdminUseCase useCase =
                new SiteAdminUseCase(siteRepository, new FakeBuildingRepositoryPort());

        SiteResult result = useCase.create("판교 사옥", "경기도 성남시");

        assertEquals("판교 사옥", result.name());
        assertEquals(1, siteRepository.sites.size());
    }

    @Test
    void deleteBlockedWhenBuildingsExist() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        UUID siteId = siteRepository.addSite();
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        buildingRepository.addBuilding(siteId);

        SiteAdminUseCase useCase = new SiteAdminUseCase(siteRepository, buildingRepository);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.delete(siteId));
        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void deleteSucceedsWhenNoBuildings() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        UUID siteId = siteRepository.addSite();

        SiteAdminUseCase useCase =
                new SiteAdminUseCase(siteRepository, new FakeBuildingRepositoryPort());

        useCase.delete(siteId);

        assertTrue(siteRepository.sites.isEmpty());
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
            UUID id = site.id() == null ? UUID.randomUUID() : site.id();
            Site stored = Site.of(id, site.affiliateId(), site.name(), site.address());
            sites.put(id, stored);
            return stored;
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

    private static class FakeBuildingRepositoryPort implements BuildingRepositoryPort {

        private final List<Building> buildings = new ArrayList<>();

        void addBuilding(UUID siteId) {
            buildings.add(Building.of(UUID.randomUUID(), siteId, "A동"));
        }

        @Override
        public Building save(Building building) {
            return building;
        }

        @Override
        public Optional<Building> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Building> findAll() {
            return List.copyOf(buildings);
        }

        @Override
        public List<Building> findBySiteId(UUID siteId) {
            return buildings.stream().filter(b -> b.siteId().equals(siteId)).toList();
        }

        @Override
        public void deleteById(UUID id) {}
    }
}
