package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 동료 일정 구독 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 중복 구독 방지를 위한 존재 조회를 제공한다. */
public interface PersonalWorkspaceCalendarSubscriptionRepositoryPort {

    PersonalWorkspaceCalendarSubscription save(PersonalWorkspaceCalendarSubscription subscription);

    Optional<PersonalWorkspaceCalendarSubscription> findByIdAndSubscriberUserId(
            UUID subscriptionId, UUID subscriberUserId);

    boolean existsBySubscriberUserIdAndTargetUserId(UUID subscriberUserId, UUID targetUserId);

    List<PersonalWorkspaceCalendarSubscription> findBySubscriberUserId(UUID subscriberUserId);

    boolean deleteByIdAndSubscriberUserId(UUID subscriptionId, UUID subscriberUserId);
}
