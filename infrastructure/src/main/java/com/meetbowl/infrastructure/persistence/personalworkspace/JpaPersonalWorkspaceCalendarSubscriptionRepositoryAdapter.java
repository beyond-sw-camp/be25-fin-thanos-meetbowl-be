package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

/**
 * 동료 일정 구독의 {@link PersonalWorkspaceCalendarSubscriptionRepositoryPort}를 JPA로 구현한다.
 *
 * <p>조회·삭제를 구독자 ID와 함께 수행해 남의 구독을 건드리지 못하게 하고, (구독자, 대상) 중복 존재 확인을 제공해 같은 동료를 두 번 구독하지 않게 한다.
 */
@Repository
public class JpaPersonalWorkspaceCalendarSubscriptionRepositoryAdapter
        implements PersonalWorkspaceCalendarSubscriptionRepositoryPort {

    private final SpringDataPersonalWorkspaceCalendarSubscriptionRepository repository;

    public JpaPersonalWorkspaceCalendarSubscriptionRepositoryAdapter(
            SpringDataPersonalWorkspaceCalendarSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PersonalWorkspaceCalendarSubscription save(
            PersonalWorkspaceCalendarSubscription subscription) {
        return repository
                .save(PersonalWorkspaceCalendarSubscriptionEntity.from(subscription))
                .toDomain();
    }

    @Override
    public Optional<PersonalWorkspaceCalendarSubscription> findByIdAndSubscriberUserId(
            UUID subscriptionId, UUID subscriberUserId) {
        return repository
                .findByIdAndSubscriberUserId(subscriptionId, subscriberUserId)
                .map(PersonalWorkspaceCalendarSubscriptionEntity::toDomain);
    }

    @Override
    public boolean existsBySubscriberUserIdAndTargetUserId(
            UUID subscriberUserId, UUID targetUserId) {
        return repository.existsBySubscriberUserIdAndTargetUserId(subscriberUserId, targetUserId);
    }

    @Override
    public List<PersonalWorkspaceCalendarSubscription> findBySubscriberUserId(
            UUID subscriberUserId) {
        return repository.findBySubscriberUserIdOrderBySubscribedAtDesc(subscriberUserId).stream()
                .map(PersonalWorkspaceCalendarSubscriptionEntity::toDomain)
                .toList();
    }

    @Override
    public boolean deleteByIdAndSubscriberUserId(UUID subscriptionId, UUID subscriberUserId) {
        return repository.deleteByIdAndSubscriberUserId(subscriptionId, subscriberUserId) > 0;
    }
}
