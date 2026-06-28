package com.meetbowl.application.meetingroom;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/** 관리자 사이트(거점) 기준정보 관리 UseCase다(F1, FR-095). 회의실 등록의 선행 데이터다. */
@Service
public class SiteAdminUseCase {

    private final SiteRepositoryPort siteRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;

    public SiteAdminUseCase(
            SiteRepositoryPort siteRepositoryPort, BuildingRepositoryPort buildingRepositoryPort) {
        this.siteRepositoryPort = siteRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<SiteResult> list() {
        return list(null);
    }

    @Transactional
    public SiteResult create(String name, String address) {
        return create(null, name, address);
    }

    @Transactional
    public SiteResult update(UUID siteId, String name, String address) {
        return update(siteId, null, name, address);
    }

    @Transactional
    public void delete(UUID siteId) {
        delete(siteId, null);
    }

    @Transactional(readOnly = true)
    public List<SiteResult> list(UUID adminAffiliateId) {
        return siteRepositoryPort.findAll().stream()
                .filter(
                        site ->
                                adminAffiliateId == null
                                        || Objects.equals(site.affiliateId(), adminAffiliateId))
                .map(SiteResult::from)
                .toList();
    }

    @Transactional
    public SiteResult create(UUID adminAffiliateId, String name, String address) {
        return SiteResult.from(siteRepositoryPort.save(Site.create(adminAffiliateId, name, address)));
    }

    @Transactional
    public SiteResult update(UUID siteId, UUID adminAffiliateId, String name, String address) {
        Site site = requireSite(siteId);
        ensureAccessible(site, adminAffiliateId);
        return SiteResult.from(
                siteRepositoryPort.save(site.change(adminAffiliateId == null ? site.affiliateId() : adminAffiliateId, name, address)));
    }

    @Transactional
    public void delete(UUID siteId, UUID adminAffiliateId) {
        Site site = requireSite(siteId);
        ensureAccessible(site, adminAffiliateId);
        if (!buildingRepositoryPort.findBySiteId(siteId).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "하위 건물이 있는 사이트는 삭제할 수 없습니다.");
        }
        siteRepositoryPort.deleteById(siteId);
    }

    private Site requireSite(UUID siteId) {
        return siteRepositoryPort
                .findById(siteId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "사이트를 찾을 수 없습니다."));
    }

    private void ensureAccessible(Site site, UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            return;
        }
        if (!Objects.equals(site.affiliateId(), adminAffiliateId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "다른 계열사의 사이트는 관리할 수 없습니다.");
        }
    }
}
