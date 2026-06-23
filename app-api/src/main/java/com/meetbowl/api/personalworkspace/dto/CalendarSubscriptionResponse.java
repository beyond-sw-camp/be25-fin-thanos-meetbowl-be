package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.calendar.CalendarSubscriptionResult;

/** 동료 일정 구독 응답 본문이다. 구독 결과를 클라이언트 계약으로 변환해 노출한다. */
public record CalendarSubscriptionResponse(
        UUID subscriptionId, UUID subscriberUserId, UUID targetUserId, Instant subscribedAt) {

    public static CalendarSubscriptionResponse from(CalendarSubscriptionResult result) {
        return new CalendarSubscriptionResponse(
                result.subscriptionId(),
                result.subscriberUserId(),
                result.targetUserId(),
                result.subscribedAt());
    }
}
