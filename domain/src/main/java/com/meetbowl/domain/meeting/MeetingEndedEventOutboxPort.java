package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** meeting.ended 이벤트를 DB 트랜잭션 안에서 보관하고 발행 상태를 갱신하는 Outbox Port다. */
public interface MeetingEndedEventOutboxPort {

    void save(MeetingEndedEvent event);

    List<MeetingEndedEvent> findReadyToPublish(Instant now, int limit);

    void removePublished(UUID eventId);

    void markFailed(UUID eventId, Instant nextAttemptAt, String failureReason);
}
