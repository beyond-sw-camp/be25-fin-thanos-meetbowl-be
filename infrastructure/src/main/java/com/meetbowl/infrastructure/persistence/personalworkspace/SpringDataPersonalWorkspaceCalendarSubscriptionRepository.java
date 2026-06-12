package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/** 동료 일정 구독 엔티티의 Spring Data JPA 리포지토리다. 중복 구독 방지를 위한 (구독자, 대상) 존재 조회를 제공한다. */
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
