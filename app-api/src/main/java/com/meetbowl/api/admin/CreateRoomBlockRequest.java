package com.meetbowl.api.admin;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 회의실 시간대 차단 등록 요청이다. 구간은 {@code [startAt, endAt)}(UTC ISO-8601). */
public record CreateRoomBlockRequest(
        @NotNull(message = "차단 시작 시각은 필수입니다.") Instant startAt,
        @NotNull(message = "차단 종료 시각은 필수입니다.") Instant endAt,
        @Size(max = 200, message = "차단 사유는 200자 이하여야 합니다.") String reason) {}
