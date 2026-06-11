package com.meetbowl.application.personalworkspace.calendar;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

@Service
public class GetCalendarSubscriptionsUseCase {

    private final PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionRepositoryPort;

    public GetCalendarSubscriptionsUseCase(
            PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionRepositoryPort) {
        this.subscriptionRepositoryPort = subscriptionRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<CalendarSubscriptionResult> execute(UUID subscriberUserId) {
        return subscriptionRepositoryPort.findBySubscriberUserId(subscriberUserId).stream()
                .map(CalendarSubscriptionResult::from)
                .toList();
    }
}
