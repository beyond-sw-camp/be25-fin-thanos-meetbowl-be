package com.meetbowl.infrastructure.persistence.minutes;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMinutesGeneratedEventRepository
        extends JpaRepository<MinutesGeneratedEventEntity, UUID> {

    boolean existsByEventId(UUID eventId);
}
