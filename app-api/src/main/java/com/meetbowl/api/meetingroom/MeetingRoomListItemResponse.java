package com.meetbowl.api.meetingroom;

import java.util.UUID;

import com.meetbowl.application.meetingroom.MeetingRoomListItemResult;

/** 회의실 목록 항목 응답 DTO다(F3). */
public record MeetingRoomListItemResponse(
        UUID roomId,
        String name,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        Integer floor,
        String location,
        int capacity,
        boolean isAvailable) {

    public static MeetingRoomListItemResponse from(MeetingRoomListItemResult result) {
        return new MeetingRoomListItemResponse(
                result.roomId(),
                result.name(),
                result.siteId(),
                result.siteName(),
                result.buildingId(),
                result.buildingName(),
                result.floor(),
                result.location(),
                result.capacity(),
                result.available());
    }
}