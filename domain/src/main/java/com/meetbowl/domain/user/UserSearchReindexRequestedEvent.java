package com.meetbowl.domain.user;

import java.util.List;
import java.util.UUID;

/** 회원 검색 인덱스 갱신을 RabbitMQ consumer로 넘기기 위한 도메인 이벤트다. */
public record UserSearchReindexRequestedEvent(
        String reason,
        boolean reindexAll,
        List<UUID> userIds,
        UUID affiliateId,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        UUID requestedByUserId) {

    public UserSearchReindexRequestedEvent {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
