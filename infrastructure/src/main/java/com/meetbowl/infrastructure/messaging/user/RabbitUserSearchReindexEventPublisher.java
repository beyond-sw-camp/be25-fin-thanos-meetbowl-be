package com.meetbowl.infrastructure.messaging.user;

import org.springframework.stereotype.Component;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.common.event.user.UserSearchReindexRequestedMessage;
import com.meetbowl.domain.user.UserSearchReindexEventPublisherPort;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** 회원 검색 재색인 요청을 계약 Message DTO로 변환해 공통 RabbitMQ 발행기에 전달한다. */
@Component
public class RabbitUserSearchReindexEventPublisher implements UserSearchReindexEventPublisherPort {

    private final RabbitEventPublisher rabbitEventPublisher;

    public RabbitUserSearchReindexEventPublisher(RabbitEventPublisher rabbitEventPublisher) {
        this.rabbitEventPublisher = rabbitEventPublisher;
    }

    @Override
    public void publish(UserSearchReindexRequestedEvent event) {
        rabbitEventPublisher.publish(
                EventTypes.USER_SEARCH_REINDEX_REQUESTED,
                new UserSearchReindexRequestedMessage(
                        event.reason(),
                        event.reindexAll(),
                        event.userIds(),
                        event.affiliateId(),
                        event.departmentId(),
                        event.teamId(),
                        event.positionId(),
                        event.requestedByUserId()));
    }
}
