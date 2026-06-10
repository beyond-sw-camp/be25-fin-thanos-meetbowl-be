package com.meetbowl.domain.meetingroom;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 건물 기준정보 도메인 모델이다.
 *
 * <p>사이트(Site)에 속하며 회의실(MeetingRoom)을 가진다. 관리자가 회의실 등록 화면에서 추가하고 사이트에 연결한다(FR-095). 사이트는 {@code
 * siteId} raw UUID로 참조한다.
 */
public class Building {

    private final UUID id;

    /** 소속 사이트(FK). */
    private final UUID siteId;

    /** 건물명(필수). */
    private final String name;

    private Building(UUID id, UUID siteId, String name) {
        this.id = id;
        this.siteId = siteId;
        this.name = name;
    }

    public static Building create(UUID siteId, String name) {
        return of(null, siteId, name);
    }

    public static Building of(UUID id, UUID siteId, String name) {
        if (siteId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "소속 사이트는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "건물명은 필수입니다.");
        }
        return new Building(id, siteId, name);
    }

    public Building change(UUID newSiteId, String newName) {
        return of(id, newSiteId, newName);
    }

    public UUID id() {
        return id;
    }

    public UUID siteId() {
        return siteId;
    }

    public String name() {
        return name;
    }
}
