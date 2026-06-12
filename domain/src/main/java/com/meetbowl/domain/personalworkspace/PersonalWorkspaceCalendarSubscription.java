package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 동료 일정 구독 애그리거트다. 구독자와 대상 사용자를 잇고, 자기 자신 구독을 금지하는 규칙을 생성 시점에 강제한다. */
public class PersonalWorkspaceCalendarSubscription {

    private final UUID id;
    private final UUID subscriberUserId;
    private final UUID targetUserId;
    private final Instant subscribedAt;

    private PersonalWorkspaceCalendarSubscription(
            UUID id, UUID subscriberUserId, UUID targetUserId, Instant subscribedAt) {
        this.id = id;
        this.subscriberUserId = subscriberUserId;
        this.targetUserId = targetUserId;
        this.subscribedAt = subscribedAt;
    }

    public static PersonalWorkspaceCalendarSubscription create(
            UUID subscriberUserId, UUID targetUserId, Instant subscribedAt) {
        return of(null, subscriberUserId, targetUserId, subscribedAt);
    }

    public static PersonalWorkspaceCalendarSubscription of(
            UUID id, UUID subscriberUserId, UUID targetUserId, Instant subscribedAt) {
        PersonalWorkspaceCalendarEvent.requireId(subscriberUserId, "구독자 ID는 필수입니다.");
        PersonalWorkspaceCalendarEvent.requireId(targetUserId, "구독 대상 사용자 ID는 필수입니다.");
        if (subscriberUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "자기 자신의 일정은 구독할 수 없습니다.");
        }
        if (subscribedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "구독 시각은 필수입니다.");
        }

        return new PersonalWorkspaceCalendarSubscription(
                id, subscriberUserId, targetUserId, subscribedAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return subscriberUserId.equals(userId);
    }

    public UUID id() {
        return id;
    }

    public UUID subscriberUserId() {
        return subscriberUserId;
    }

    public UUID targetUserId() {
        return targetUserId;
    }

    public Instant subscribedAt() {
        return subscribedAt;
    }
}
