package com.meetbowl.application.meetingroom;

import java.util.UUID;

/**
 * 회의실 목록 조회 입력 모델이다(F3). 모든 필터는 선택이며 null이면 무시한다. {@code page}는 1부터 시작한다.
 */
public record GetMeetingRoomsQuery(
        UUID siteId, UUID buildingId, Integer minCapacity, int page, int size) {}