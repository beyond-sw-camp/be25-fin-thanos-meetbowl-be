package com.meetbowl.infrastructure.persistence.minutes;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** MinutesRepositoryPort를 Spring Data JPA로 구현하는 persistence adapter다. */
@Repository
public class JpaMinutesRepositoryAdapter implements MinutesRepositoryPort {

    private final SpringDataMinutesRepository springDataMinutesRepository;

    public JpaMinutesRepositoryAdapter(SpringDataMinutesRepository springDataMinutesRepository) {
        this.springDataMinutesRepository = springDataMinutesRepository;
    }

    @Override
    @Transactional
    public Minutes save(Minutes minutes) {
        MinutesEntity savedEntity = springDataMinutesRepository.save(MinutesEntity.from(minutes));
        return savedEntity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Minutes> findById(UUID minutesId) {
        return springDataMinutesRepository.findById(minutesId).map(MinutesEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Minutes> findByMeetingId(UUID meetingId) {
        return springDataMinutesRepository.findByMeetingId(meetingId).map(MinutesEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMeetingId(UUID meetingId) {
        return springDataMinutesRepository.existsByMeetingId(meetingId);
    }
}
