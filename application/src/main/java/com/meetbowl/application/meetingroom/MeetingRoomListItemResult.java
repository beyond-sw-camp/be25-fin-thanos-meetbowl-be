package com.meetbowl.application.meetingroom;

import java.util.UUID;

import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.Site;

/** 회의실 목록 항목 출력 모델이다(F3). 사이트/건물명을 함께 담는다. */
public record MeetingRoomListItemResult(
        UUID roomId,
        String name,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        Integer floor,
        String location,
        int capacity,
        boolean available) {

    public static MeetingRoomListItemResult of(MeetingRoom room, Building building, Site site) {
        return new MeetingRoomListItemResult(
                room.id(),
                room.name(),
                site == null ? null : site.id(),
                site == null ? null : site.name(),
                room.buildingId(),
                building == null ? null : building.name(),
                room.floor(),
                room.location(),
                room.capacity(),
                room.isAvailable());
    }
}