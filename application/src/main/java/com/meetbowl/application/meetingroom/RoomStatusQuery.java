package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.UUID;

/**
 * 회의실 현황 조회 입력 모델이다(F3). {@code from}~{@code to}는 현황을 볼 시간대(예약하려는 슬롯)이며 {@code from < to}여야 한다.
 * {@code siteId}/{@code buildingId}가 null이면 해당 필터는 무시한다.
 */
public record RoomStatusQuery(Instant from, Instant to, UUID siteId, UUID buildingId) {}