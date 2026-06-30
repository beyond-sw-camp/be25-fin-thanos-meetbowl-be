package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.meetingroom.RoomBlock;

/** 회의실 시간대 차단 출력 모델이다. app-api는 이 Result를 API Response DTO로 변환한다. */
public record RoomBlockResult(
        UUID blockId, UUID roomId, Instant startAt, Instant endAt, String reason) {

    public static RoomBlockResult of(RoomBlock block) {
        return new RoomBlockResult(
                block.id(), block.roomId(), block.startAt(), block.endAt(), block.reason());
    }
}
