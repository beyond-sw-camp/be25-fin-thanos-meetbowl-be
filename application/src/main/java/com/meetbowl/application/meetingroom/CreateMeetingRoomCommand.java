package com.meetbowl.application.meetingroom;

import java.util.UUID;

/** 회의실 등록 입력 모델이다(F2). */
public record CreateMeetingRoomCommand(
        UUID buildingId,
        String name,
        Integer floor,
        String location,
        int capacity,
        boolean available) {}
