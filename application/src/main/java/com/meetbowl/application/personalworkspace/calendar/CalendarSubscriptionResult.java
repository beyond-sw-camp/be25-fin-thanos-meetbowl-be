package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;

public record CalendarSubscriptionResult(
        UUID subscriptionId, UUID subscriberUserId, UUID targetUserId, Instant subscribedAt) {

    public static CalendarSubscriptionResult from(
            PersonalWorkspaceCalendarSubscription subscription) {
        return new CalendarSubscriptionResult(
                subscription.id(),
                subscription.subscriberUserId(),
                subscription.targetUserId(),
                subscription.subscribedAt());
    }
}
