package com.meetbowl.domain.minutes;

import java.util.UUID;

/** 처리 완료한 minutes.generated eventId를 영속화하는 Consumer inbox Port다. */
public interface MinutesGeneratedEventRepositoryPort {

    boolean existsByEventId(UUID eventId);

    void save(UUID eventId, UUID meetingId);
}
