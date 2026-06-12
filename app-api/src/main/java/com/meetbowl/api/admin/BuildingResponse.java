package com.meetbowl.api.admin;

import java.util.UUID;

import com.meetbowl.application.meetingroom.BuildingResult;

/** 건물 응답 DTO다(F1). */
public record BuildingResponse(UUID buildingId, UUID siteId, String name) {

    public static BuildingResponse from(BuildingResult result) {
        return new BuildingResponse(result.buildingId(), result.siteId(), result.name());
    }
}
