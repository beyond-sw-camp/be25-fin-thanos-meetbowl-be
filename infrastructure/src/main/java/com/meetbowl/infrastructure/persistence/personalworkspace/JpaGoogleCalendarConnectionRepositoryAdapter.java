package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.GoogleCalendarConnection;
import com.meetbowl.domain.personalworkspace.GoogleCalendarConnectionRepositoryPort;

@Repository
public class JpaGoogleCalendarConnectionRepositoryAdapter
        implements GoogleCalendarConnectionRepositoryPort {

    private final SpringDataGoogleCalendarConnectionRepository repository;

    public JpaGoogleCalendarConnectionRepositoryAdapter(
            SpringDataGoogleCalendarConnectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public GoogleCalendarConnection save(GoogleCalendarConnection connection) {
        return repository.save(GoogleCalendarConnectionEntity.from(connection)).toDomain();
    }

    @Override
    public Optional<GoogleCalendarConnection> findActiveByOwnerUserId(UUID ownerUserId) {
        return repository
                .findFirstByOwnerUserIdAndDisconnectedAtIsNullOrderByConnectedAtDesc(ownerUserId)
                .map(GoogleCalendarConnectionEntity::toDomain);
    }
}
