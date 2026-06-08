package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPersonalWorkspaceCalendarSubscriptionRepository
        extends JpaRepository<PersonalWorkspaceCalendarSubscriptionEntity, UUID> {

    boolean existsBySubscriberUserIdAndTargetUserId(UUID subscriberUserId, UUID targetUserId);

    List<PersonalWorkspaceCalendarSubscriptionEntity> findBySubscriberUserIdOrderBySubscribedAtDesc(
            UUID subscriberUserId);
}
