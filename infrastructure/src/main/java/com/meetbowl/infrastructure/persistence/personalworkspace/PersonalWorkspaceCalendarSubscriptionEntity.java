package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "personal_workspace_calendar_subscriptions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_personal_workspace_calendar_subscription_pair",
                        columnNames = {"subscriber_user_id", "target_user_id"}))
public class PersonalWorkspaceCalendarSubscriptionEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID subscriberUserId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetUserId;

    @Column(nullable = false)
    private Instant subscribedAt;

    protected PersonalWorkspaceCalendarSubscriptionEntity() {}

    private PersonalWorkspaceCalendarSubscriptionEntity(
            UUID subscriberUserId, UUID targetUserId, Instant subscribedAt) {
        this.subscriberUserId = subscriberUserId;
        this.targetUserId = targetUserId;
        this.subscribedAt = subscribedAt;
    }

    static PersonalWorkspaceCalendarSubscriptionEntity from(
            PersonalWorkspaceCalendarSubscription subscription) {
        PersonalWorkspaceCalendarSubscriptionEntity entity =
                new PersonalWorkspaceCalendarSubscriptionEntity(
                        subscription.subscriberUserId(),
                        subscription.targetUserId(),
                        subscription.subscribedAt());
        entity.setId(subscription.id());
        return entity;
    }

    PersonalWorkspaceCalendarSubscription toDomain() {
        return PersonalWorkspaceCalendarSubscription.of(
                getId(), subscriberUserId, targetUserId, subscribedAt);
    }
}
