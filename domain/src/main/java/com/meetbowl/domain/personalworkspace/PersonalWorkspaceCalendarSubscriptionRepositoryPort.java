package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceCalendarSubscriptionRepositoryPort {

    PersonalWorkspaceCalendarSubscription save(PersonalWorkspaceCalendarSubscription subscription);

    Optional<PersonalWorkspaceCalendarSubscription> findById(UUID subscriptionId);

    boolean existsBySubscriberUserIdAndTargetUserId(UUID subscriberUserId, UUID targetUserId);

    List<PersonalWorkspaceCalendarSubscription> findBySubscriberUserId(UUID subscriberUserId);

    void deleteById(UUID subscriptionId);
}
