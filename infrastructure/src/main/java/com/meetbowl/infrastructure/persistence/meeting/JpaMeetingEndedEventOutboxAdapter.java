package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.domain.meeting.MeetingEndedEventOutboxPort;

/** meeting.ended Outbox의 저장·잠금 조회·발행 상태 갱신 구현이다. */
@Component
public class JpaMeetingEndedEventOutboxAdapter implements MeetingEndedEventOutboxPort {

    private final SpringDataMeetingEndedOutboxRepository repository;

    public JpaMeetingEndedEventOutboxAdapter(SpringDataMeetingEndedOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(MeetingEndedEvent event) {
        repository.save(MeetingEndedOutboxEntity.from(event));
    }

    @Override
    public List<MeetingEndedEvent> findReadyToPublish(Instant now, int limit) {
        return repository.findReadyToPublish(now, PageRequest.of(0, limit)).stream()
                .map(MeetingEndedOutboxEntity::toDomain)
                .toList();
    }

    @Override
    public void removePublished(UUID eventId) {
        repository.delete(requireEntity(eventId));
    }

    @Override
    public void markFailed(UUID eventId, Instant nextAttemptAt, String failureReason) {
        requireEntity(eventId).markFailed(nextAttemptAt, failureReason);
    }

    private MeetingEndedOutboxEntity requireEntity(UUID eventId) {
        return repository
                .findById(eventId)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "meeting.ended Outbox를 찾을 수 없습니다. eventId=" + eventId));
    }
}
