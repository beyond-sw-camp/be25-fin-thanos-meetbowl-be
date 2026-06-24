package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataMeetingEndedOutboxRepository
        extends JpaRepository<MeetingEndedOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select o from MeetingEndedOutboxEntity o"
                    + " where o.nextAttemptAt <= :now"
                    + " order by o.createdAt asc")
    List<MeetingEndedOutboxEntity> findReadyToPublish(@Param("now") Instant now, Pageable pageable);
}
