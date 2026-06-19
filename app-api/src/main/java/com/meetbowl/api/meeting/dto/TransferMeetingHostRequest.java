package com.meetbowl.api.meeting.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** 회의 관리자를 다른 참석자로 이전할 때 사용하는 요청 DTO다. */
public record TransferMeetingHostRequest(
        @NotNull(message = "newHostUserId는 필수입니다.") UUID newHostUserId) {}
