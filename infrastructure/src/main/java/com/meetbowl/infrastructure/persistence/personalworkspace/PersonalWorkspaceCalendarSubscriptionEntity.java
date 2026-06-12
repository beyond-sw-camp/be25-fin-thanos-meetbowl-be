package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * {@link PersonalWorkspaceCalendarSubscription}을 저장하는 영속 엔티티다. (구독자, 대상) 유니크 제약으로 같은 동료의 중복 구독을
 * 막는다.
 */
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
