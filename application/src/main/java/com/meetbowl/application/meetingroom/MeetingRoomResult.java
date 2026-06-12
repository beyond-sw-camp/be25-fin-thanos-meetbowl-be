package com.meetbowl.application.meetingroom;

import java.util.UUID;

import com.meetbowl.domain.meetingroom.MeetingRoom;

/** 회의실 출력 모델이다(F2). app-api는 이 Result를 API Response DTO로 변환한다. */
public record MeetingRoomResult(
        UUID roomId,
        UUID buildingId,
        String name,
        Integer floor,
        String location,
        int capacity,
        boolean available) {

    public static MeetingRoomResult of(MeetingRoom room) {
        return new MeetingRoomResult(
                room.id(),
                room.buildingId(),
                room.name(),
                room.floor(),
                room.location(),
                room.capacity(),
                room.isAvailable());
    }
}