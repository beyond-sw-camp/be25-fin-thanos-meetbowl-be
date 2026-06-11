package com.meetbowl.application.personalworkspace.calendar;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

/** 구독자 본인 소유 구독만 해제할 수 있도록 조회 범위를 제한한다. */
@Service
public class UnsubscribeCalendarUseCase {

    private final PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionRepositoryPort;

    public UnsubscribeCalendarUseCase(
            PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionRepositoryPort) {
        this.subscriptionRepositoryPort = subscriptionRepositoryPort;
    }

    @Transactional
    public void execute(UUID subscriberUserId, UUID subscriptionId) {
        boolean deleted =
                subscriptionRepositoryPort.deleteByIdAndSubscriberUserId(
                        subscriptionId, subscriberUserId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "해제할 구독을 찾을 수 없습니다.");
        }
    }
}
