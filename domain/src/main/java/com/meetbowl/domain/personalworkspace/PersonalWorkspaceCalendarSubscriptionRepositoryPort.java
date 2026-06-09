package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceCalendarSubscriptionRepositoryPort {

    PersonalWorkspaceCalendarSubscription save(PersonalWorkspaceCalendarSubscription subscription);

    Optional<PersonalWorkspaceCalendarSubscription> findByIdAndSubscriberUserId(
            UUID subscriptionId, UUID subscriberUserId);

    boolean existsBySubscriberUserIdAndTargetUserId(UUID subscriberUserId, UUID targetUserId);

    List<PersonalWorkspaceCalendarSubscription> findBySubscriberUserId(UUID subscriberUserId);

    boolean deleteByIdAndSubscriberUserId(UUID subscriptionId, UUID subscriberUserId);
}
