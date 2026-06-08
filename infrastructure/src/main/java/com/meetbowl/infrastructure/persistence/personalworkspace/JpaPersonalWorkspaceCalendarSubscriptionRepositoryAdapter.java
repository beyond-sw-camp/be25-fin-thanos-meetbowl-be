package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

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
    public Optional<PersonalWorkspaceCalendarSubscription> findById(UUID subscriptionId) {
        return repository
                .findById(subscriptionId)
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
    public void deleteById(UUID subscriptionId) {
        repository.deleteById(subscriptionId);
    }
}
