package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

/**
 * 동료 일정 구독을 등록한다.
 *
 * <p>자기 자신 구독 금지는 도메인이 검증한다. 같은 대상에 대한 중복 구독은 유일해야 하므로 이미 구독 중이면 충돌로 처리해 동일 구독이 여러 건 쌓이지 않게 한다.
 */
@Service
public class SubscribeCalendarUseCase {

    private final PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionRepositoryPort;

    public SubscribeCalendarUseCase(
            PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionRepositoryPort) {
        this.subscriptionRepositoryPort = subscriptionRepositoryPort;
    }

    @Transactional
    public CalendarSubscriptionResult execute(SubscribeCalendarCommand command) {
        if (subscriptionRepositoryPort.existsBySubscriberUserIdAndTargetUserId(
                command.subscriberUserId(), command.targetUserId())) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 구독한 동료 일정입니다.");
        }

        PersonalWorkspaceCalendarSubscription subscription =
                PersonalWorkspaceCalendarSubscription.create(
                        command.subscriberUserId(), command.targetUserId(), Instant.now());

        return CalendarSubscriptionResult.from(subscriptionRepositoryPort.save(subscription));
    }
}
