package com.meetbowl.application.meetingroom;

import java.util.UUID;

import com.meetbowl.domain.meetingroom.Site;

/** 사이트(거점) 출력 모델이다(F1). */
public record SiteResult(UUID siteId, String name, String address) {

    public static SiteResult from(Site site) {
        return new SiteResult(site.id(), site.name(), site.address());
    }
}
