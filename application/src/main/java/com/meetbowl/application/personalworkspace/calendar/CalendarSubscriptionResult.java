package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;

/** 동료 일정 구독 결과다. 구독자·대상 사용자와 구독 시각을 담는다. */
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
