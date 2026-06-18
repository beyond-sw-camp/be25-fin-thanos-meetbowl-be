package com.meetbowl.common.event.user;

import java.util.List;
import java.util.UUID;

/** 루트 event-contract의 user.search.reindex.requested payload를 표현하는 RabbitMQ Message DTO다. */
public record UserSearchReindexRequestedMessage(
        String reason,
        boolean reindexAll,
        List<UUID> userIds,
        UUID affiliateId,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        UUID requestedByUserId) {

    public UserSearchReindexRequestedMessage {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
