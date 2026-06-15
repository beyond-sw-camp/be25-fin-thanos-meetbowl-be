package com.meetbowl.application.meetingroom;

import java.util.UUID;

/** 사이트+건물 동시 생성 결과다(F1 보강). app-api는 이 Result를 Response DTO로 변환한다. */
public record SiteWithBuildingResult(
        UUID siteId, String siteName, UUID buildingId, String buildingName) {}
