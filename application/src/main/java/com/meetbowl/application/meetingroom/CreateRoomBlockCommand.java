package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.UUID;

/** 회의실 시간대 차단 등록 입력 모델이다. 구간은 {@code [startAt, endAt)}(UTC). */
public record CreateRoomBlockCommand(UUID roomId, Instant startAt, Instant endAt, String reason) {}
