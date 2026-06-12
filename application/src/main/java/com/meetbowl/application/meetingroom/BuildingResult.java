package com.meetbowl.application.meetingroom;

import java.util.UUID;

import com.meetbowl.domain.meetingroom.Building;

/** 건물 출력 모델이다(F1). */
public record BuildingResult(UUID buildingId, UUID siteId, String name) {

    public static BuildingResult from(Building building) {
        return new BuildingResult(building.id(), building.siteId(), building.name());
    }
}
