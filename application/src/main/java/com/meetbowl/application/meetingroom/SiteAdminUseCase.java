package com.meetbowl.application.meetingroom;

import java.util.List;
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
        return siteRepositoryPort.findAll().stream().map(SiteResult::from).toList();
    }

    @Transactional
    public SiteResult create(String name, String address) {
        return SiteResult.from(siteRepositoryPort.save(Site.create(name, address)));
    }

    @Transactional
    public SiteResult update(UUID siteId, String name, String address) {
        Site site = requireSite(siteId);
        return SiteResult.from(siteRepositoryPort.save(site.change(name, address)));
    }

    @Transactional
    public void delete(UUID siteId) {
        requireSite(siteId);
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
}
