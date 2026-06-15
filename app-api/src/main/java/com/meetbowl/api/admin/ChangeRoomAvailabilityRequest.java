package com.meetbowl.api.admin;

import jakarta.validation.constraints.NotNull;

/** 회의실 사용 가능 여부 변경 요청 DTO다(F2, FR-089). */
public record ChangeRoomAvailabilityRequest(
        @NotNull(message = "사용 가능 여부는 필수입니다.") Boolean isAvailable) {}
