package com.meetbowl.infrastructure.persistence.minutes;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.minutes.MinutesGeneratedEventRepositoryPort;

/** minutes.generated inbox Port의 JPA 구현이다. */
@Repository
public class JpaMinutesGeneratedEventRepositoryAdapter
        implements MinutesGeneratedEventRepositoryPort {

    private final SpringDataMinutesGeneratedEventRepository repository;

    public JpaMinutesGeneratedEventRepositoryAdapter(
            SpringDataMinutesGeneratedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public void save(UUID eventId, UUID meetingId) {
        repository.save(new MinutesGeneratedEventEntity(eventId, meetingId));
    }
}
