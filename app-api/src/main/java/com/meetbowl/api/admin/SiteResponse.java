package com.meetbowl.api.admin;

import java.util.UUID;

import com.meetbowl.application.meetingroom.SiteResult;

/** 사이트 응답 DTO다(F1). */
public record SiteResponse(UUID siteId, String name, String address) {

    public static SiteResponse from(SiteResult result) {
        return new SiteResponse(result.siteId(), result.name(), result.address());
    }
}
