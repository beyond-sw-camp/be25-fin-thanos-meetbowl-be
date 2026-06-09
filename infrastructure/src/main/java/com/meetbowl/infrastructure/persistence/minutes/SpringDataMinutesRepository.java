package com.meetbowl.infrastructure.persistence.minutes;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA 쿼리 메서드만 제공하는 내부 repository다. domain/application 계층에서는 직접 사용하지 않는다. */
interface SpringDataMinutesRepository extends JpaRepository<MinutesEntity, UUID> {

    Optional<MinutesEntity> findByMeetingId(UUID meetingId);

    boolean existsByMeetingId(UUID meetingId);
}
