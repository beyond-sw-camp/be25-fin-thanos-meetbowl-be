package com.meetbowl.api.admin;

import java.util.UUID;

import com.meetbowl.application.meetingroom.SiteWithBuildingResult;

/** 사이트+건물 동시 등록 응답 DTO다(F1 보강). */
public record SiteWithBuildingResponse(
        UUID siteId, String siteName, UUID buildingId, String buildingName) {

    public static SiteWithBuildingResponse from(SiteWithBuildingResult result) {
        return new SiteWithBuildingResponse(
                result.siteId(), result.siteName(), result.buildingId(), result.buildingName());
    }
}