package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMeetingExternalInviteeRepository
        extends JpaRepository<MeetingExternalInviteeEntity, UUID> {

    List<MeetingExternalInviteeEntity> findByMeetingIdOrderByCreatedAtAsc(UUID meetingId);

    void deleteByMeetingId(UUID meetingId);
}
