package com.meetbowl.api.personalworkspace.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** 동료 일정 구독 등록 요청 DTO다. 구독자는 인증 사용자에서 가져오므로 대상만 받는다. */
public record SubscribeCalendarRequest(
        @NotNull(message = "구독 대상 사용자 ID는 필수입니다.") UUID targetUserId) {}
