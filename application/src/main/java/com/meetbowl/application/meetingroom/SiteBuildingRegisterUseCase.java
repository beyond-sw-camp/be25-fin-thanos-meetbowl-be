package com.meetbowl.application.meetingroom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/**
 * 사이트와 건물을 한 폼에서 함께 등록하는 UseCase다(F1 보강). "사이트/건물 추가" 화면이 사이트명·건물명을 한 번에 보내면, 사이트를 먼저 생성해 발급된
 * siteId로 건물을 연결해 저장한다.
 *
 * <p>트랜잭션 경계를 이 UseCase에 두어 사이트 저장과 건물 저장을 하나의 트랜잭션으로 묶는다. 둘 중 하나라도 실패하면 전체 롤백되어, "사이트만 생기고 건물이
 * 누락된" 중간 상태가 남지 않는다. 새 도메인/엔티티 없이 기존 {@link Site}/{@link Building} 도메인과 두 저장소 Port를 재사용한다.
 */
@Service
public class SiteBuildingRegisterUseCase {

    private final SiteRepositoryPort siteRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;

    public SiteBuildingRegisterUseCase(
            SiteRepositoryPort siteRepositoryPort, BuildingRepositoryPort buildingRepositoryPort) {
        this.siteRepositoryPort = siteRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
    }

    /**
     * 사이트와 건물을 함께 생성한다. 입력 검증(사이트명/건물명 필수)은 각 도메인 생성 시 수행된다. 주소는 이 화면에서 받지 않으므로 null로 둔다(기존 사이트 등록과
     * 동일하게 선택 항목).
     */
    @Transactional
    public SiteWithBuildingResult execute(String siteName, String buildingName) {
        Site site = siteRepositoryPort.save(Site.create(siteName, null));
        Building building = buildingRepositoryPort.save(Building.create(site.id(), buildingName));
        return new SiteWithBuildingResult(site.id(), site.name(), building.id(), building.name());
    }
}
