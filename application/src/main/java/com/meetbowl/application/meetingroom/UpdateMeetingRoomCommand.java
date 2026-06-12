package com.meetbowl.application.meetingroom;

import java.util.UUID;

/** 회의실 수정 입력 모델이다(F2). 사용 가능 여부는 별도 API로 다루므로 여기서는 변경하지 않는다. */
public record UpdateMeetingRoomCommand(
        UUID roomId,
        UUID buildingId,
        String name,
        Integer floor,
        String location,
        int capacity) {}