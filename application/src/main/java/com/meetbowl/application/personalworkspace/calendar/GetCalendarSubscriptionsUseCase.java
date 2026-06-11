package com.meetbowl.application.personalworkspace.calendar;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

/** 현재 사용자가 구독 중인 동료 일정 목록을 조회한다. 구독자 본인 기준으로만 조회해 다른 사용자의 구독 정보를 노출하지 않는다. */
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
