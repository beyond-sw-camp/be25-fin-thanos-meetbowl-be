package com.meetbowl.api.admin;

import java.util.UUID;

import com.meetbowl.application.meetingroom.MeetingRoomResult;

/** 회의실 응답 DTO다. application Result를 외부 응답 계약에 맞게 변환한다. */
public record MeetingRoomResponse(
        UUID roomId,
        UUID buildingId,
        String name,
        Integer floor,
        String location,
        int capacity,
        boolean isAvailable) {

    public static MeetingRoomResponse from(MeetingRoomResult result) {
        return new MeetingRoomResponse(
                result.roomId(),
                result.buildingId(),
                result.name(),
                result.floor(),
                result.location(),
                result.capacity(),
                result.available());
    }
}