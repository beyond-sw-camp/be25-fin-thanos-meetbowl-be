package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 회의실 수정 요청 DTO다(F2). 사용 가능 여부는 별도 API로 변경한다. */
public record UpdateMeetingRoomRequest(
        @NotNull(message = "소속 건물은 필수입니다.") UUID buildingId,
        @NotBlank(message = "회의실명은 필수입니다.") String name,
        Integer floor,
        String location,
        @Positive(message = "수용 인원은 1명 이상이어야 합니다.") int capacity) {}
