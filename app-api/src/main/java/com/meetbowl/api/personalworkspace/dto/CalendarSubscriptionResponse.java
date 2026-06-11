package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.calendar.CalendarSubscriptionResult;

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
