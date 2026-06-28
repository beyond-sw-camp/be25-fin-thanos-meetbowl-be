package com.meetbowl.application.meetingroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class SiteBuildingRegisterUseCaseTest {

    @Test
    void executeCreatesSiteAndLinkedBuilding() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        SiteBuildingRegisterUseCase useCase =
                new SiteBuildingRegisterUseCase(siteRepository, buildingRepository);

        SiteWithBuildingResult result = useCase.execute("판교 사옥", "A동");

        // 사이트·건물이 각각 한 건씩 저장된다.
        assertEquals(1, siteRepository.sites.size());
        assertEquals(1, buildingRepository.buildings.size());

        // 응답은 DB에 저장된 이름과 발급된 id를 그대로 반환한다.
        assertNotNull(result.siteId());
        assertNotNull(result.buildingId());
        assertEquals("판교 사옥", result.siteName());
        assertEquals("A동", result.buildingName());
    }

    @Test
    void buildingIsLinkedToCreatedSiteId() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        SiteBuildingRegisterUseCase useCase =
                new SiteBuildingRegisterUseCase(siteRepository, buildingRepository);

        SiteWithBuildingResult result = useCase.execute("판교 사옥", "A동");

        // 핵심: 저장된 건물의 site_id가 방금 생성된 사이트 id로 연결돼야 한다(드롭다운 자동 채움의 전제).
        Building savedBuilding = buildingRepository.buildings.get(0);
        assertEquals(result.siteId(), savedBuilding.siteId());
    }

    @Test
    void executeFailsWhenSiteNameBlank() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        SiteBuildingRegisterUseCase useCase =
                new SiteBuildingRegisterUseCase(siteRepository, buildingRepository);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.execute("  ", "A동"));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
        // 사이트 생성 단계에서 막혀 아무것도 저장되지 않는다.
        assertTrue(siteRepository.sites.isEmpty());
        assertTrue(buildingRepository.buildings.isEmpty());
    }

    @Test
    void executeFailsWhenBuildingNameBlank() {
        FakeSiteRepositoryPort siteRepository = new FakeSiteRepositoryPort();
        FakeBuildingRepositoryPort buildingRepository = new FakeBuildingRepositoryPort();
        SiteBuildingRegisterUseCase useCase =
                new SiteBuildingRegisterUseCase(siteRepository, buildingRepository);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.execute("판교 사옥", " "));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
        // 건물 저장은 일어나지 않는다. (사이트만 생기는 중간 상태는 @Transactional 롤백으로 막으며,
        // 이는 트랜잭션 매니저가 있는 통합 테스트에서 보장된다 — 이 유닛 테스트는 fake라 롤백을 흉내내지 않는다.)
        assertTrue(buildingRepository.buildings.isEmpty());
    }

    private static class FakeSiteRepositoryPort implements SiteRepositoryPort {

        private final Map<UUID, Site> sites = new HashMap<>();

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

        @Override
        public Building save(Building building) {
            // 실제 adapter처럼 id를 발급해 저장한다.
            Building stored =
                    Building.of(
                            building.id() == null ? UUID.randomUUID() : building.id(),
                            building.siteId(),
                            building.name());
            buildings.add(stored);
            return stored;
        }

        @Override
        public Optional<Building> findById(UUID id) {
            return buildings.stream().filter(b -> b.id().equals(id)).findFirst();
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
        public void deleteById(UUID id) {
            buildings.removeIf(b -> b.id().equals(id));
        }
    }
}
