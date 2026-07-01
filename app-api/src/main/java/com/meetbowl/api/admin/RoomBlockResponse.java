package com.meetbowl.api.admin;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.meetingroom.RoomBlockResult;

/** 회의실 시간대 차단 응답이다. */
public record RoomBlockResponse(
        UUID blockId, UUID roomId, Instant startAt, Instant endAt, String reason) {

    public static RoomBlockResponse from(RoomBlockResult result) {
        return new RoomBlockResponse(
                result.blockId(),
                result.roomId(),
                result.startAt(),
                result.endAt(),
                result.reason());
    }
}
