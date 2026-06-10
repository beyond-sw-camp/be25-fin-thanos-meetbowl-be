package com.meetbowl.domain.personalworkspace;

import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarConnectionRepositoryPort {

    GoogleCalendarConnection save(GoogleCalendarConnection connection);

    Optional<GoogleCalendarConnection> findActiveByOwnerUserId(UUID ownerUserId);
}
