package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataGoogleCalendarConnectionRepository
        extends JpaRepository<GoogleCalendarConnectionEntity, UUID> {

    Optional<GoogleCalendarConnectionEntity>
            findFirstByOwnerUserIdAndDisconnectedAtIsNullOrderByConnectedAtDesc(UUID ownerUserId);
}
