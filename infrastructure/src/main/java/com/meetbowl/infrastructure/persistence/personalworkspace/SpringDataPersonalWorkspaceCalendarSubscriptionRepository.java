package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface SpringDataPersonalWorkspaceCalendarSubscriptionRepository
        extends JpaRepository<PersonalWorkspaceCalendarSubscriptionEntity, UUID> {

    Optional<PersonalWorkspaceCalendarSubscriptionEntity> findByIdAndSubscriberUserId(
            UUID subscriptionId, UUID subscriberUserId);

    boolean existsBySubscriberUserIdAndTargetUserId(UUID subscriberUserId, UUID targetUserId);

    List<PersonalWorkspaceCalendarSubscriptionEntity> findBySubscriberUserIdOrderBySubscribedAtDesc(
            UUID subscriberUserId);

    @Transactional
    long deleteByIdAndSubscriberUserId(UUID subscriptionId, UUID subscriberUserId);
}
