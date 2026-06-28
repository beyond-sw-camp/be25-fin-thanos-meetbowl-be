package com.meetbowl.application.meetingroom;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/** 관리자 건물 기준정보 관리 UseCase다(F1, FR-095). 건물은 사이트에 속하고 회의실의 선행 데이터다. */
@Service
public class BuildingAdminUseCase {

    private final BuildingRepositoryPort buildingRepositoryPort;
    private final SiteRepositoryPort siteRepositoryPort;
    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;

    public BuildingAdminUseCase(
            BuildingRepositoryPort buildingRepositoryPort,
            SiteRepositoryPort siteRepositoryPort,
            MeetingRoomRepositoryPort meetingRoomRepositoryPort) {
        this.buildingRepositoryPort = buildingRepositoryPort;
        this.siteRepositoryPort = siteRepositoryPort;
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<BuildingResult> list(UUID siteId) {
        return list(siteId, null);
    }

    @Transactional(readOnly = true)
    public List<BuildingResult> list(UUID siteId, UUID adminAffiliateId) {
        List<Site> accessibleSites =
                siteRepositoryPort.findAll().stream()
                        .filter(
                                site ->
                                        adminAffiliateId == null
                                                || Objects.equals(
                                                        site.affiliateId(), adminAffiliateId))
                        .toList();
        List<UUID> accessibleSiteIds = accessibleSites.stream().map(Site::id).toList();
        List<Building> buildings =
                siteId == null
                        ? buildingRepositoryPort.findAll().stream()
                                .filter(building -> accessibleSiteIds.contains(building.siteId()))
                                .toList()
                        : buildingRepositoryPort.findBySiteId(siteId).stream()
                                .filter(building -> accessibleSiteIds.contains(building.siteId()))
                                .toList();
        return buildings.stream().map(BuildingResult::from).toList();
    }

    @Transactional
    public BuildingResult create(UUID siteId, String name) {
        return create(siteId, name, null);
    }

    @Transactional
    public BuildingResult create(UUID siteId, String name, UUID adminAffiliateId) {
        requireSite(siteId);
        ensureSiteAccessible(siteId, adminAffiliateId);
        return BuildingResult.from(buildingRepositoryPort.save(Building.create(siteId, name)));
    }

    @Transactional
    public BuildingResult update(UUID buildingId, UUID siteId, String name) {
        return update(buildingId, siteId, name, null);
    }

    @Transactional
    public BuildingResult update(UUID buildingId, UUID siteId, String name, UUID adminAffiliateId) {
        Building building = requireBuilding(buildingId);
        ensureSiteAccessible(building.siteId(), adminAffiliateId);
        requireSite(siteId);
        ensureSiteAccessible(siteId, adminAffiliateId);
        return BuildingResult.from(buildingRepositoryPort.save(building.change(siteId, name)));
    }

    @Transactional
    public void delete(UUID buildingId) {
        delete(buildingId, null);
    }

    @Transactional
    public void delete(UUID buildingId, UUID adminAffiliateId) {
        requireBuilding(buildingId);
        ensureSiteAccessible(requireBuilding(buildingId).siteId(), adminAffiliateId);
        if (!meetingRoomRepositoryPort.findByBuildingId(buildingId).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "하위 회의실이 있는 건물은 삭제할 수 없습니다.");
        }
        buildingRepositoryPort.deleteById(buildingId);
    }

    private Building requireBuilding(UUID buildingId) {
        return buildingRepositoryPort
                .findById(buildingId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "건물을 찾을 수 없습니다."));
    }

    private void requireSite(UUID siteId) {
        if (siteRepositoryPort.findById(siteId).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "사이트를 찾을 수 없습니다.");
        }
    }

    private void ensureSiteAccessible(UUID siteId, UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            return;
        }
        Site site =
                siteRepositoryPort
                        .findById(siteId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "사이트를 찾을 수 없습니다."));
        if (!Objects.equals(site.affiliateId(), adminAffiliateId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "다른 계열사의 건물은 관리할 수 없습니다.");
        }
    }
}
