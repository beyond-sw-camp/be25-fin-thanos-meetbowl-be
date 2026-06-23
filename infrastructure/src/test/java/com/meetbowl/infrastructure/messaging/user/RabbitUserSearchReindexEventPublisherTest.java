package com.meetbowl.infrastructure.messaging.user;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.common.event.user.UserSearchReindexRequestedMessage;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

class RabbitUserSearchReindexEventPublisherTest {

    @Test
    void publishUserSearchReindexRequestedMessage() {
        RabbitEventPublisher commonPublisher = mock(RabbitEventPublisher.class);
        RabbitUserSearchReindexEventPublisher publisher =
                new RabbitUserSearchReindexEventPublisher(commonPublisher);
        UUID affiliateId = UUID.randomUUID();
        UUID requestedByUserId = UUID.randomUUID();

        publisher.publish(
                new UserSearchReindexRequestedEvent(
                        "AFFILIATE_UPDATED",
                        false,
                        List.of(),
                        affiliateId,
                        null,
                        null,
                        null,
                        requestedByUserId));

        verify(commonPublisher)
                .publish(
                        eq(EventTypes.USER_SEARCH_REINDEX_REQUESTED),
                        argThat(
                                payload -> {
                                    UserSearchReindexRequestedMessage message =
                                            (UserSearchReindexRequestedMessage) payload;
                                    return "AFFILIATE_UPDATED".equals(message.reason())
                                            && !message.reindexAll()
                                            && message.userIds().isEmpty()
                                            && affiliateId.equals(message.affiliateId())
                                            && requestedByUserId.equals(
                                                    message.requestedByUserId());
                                }));
    }
}
