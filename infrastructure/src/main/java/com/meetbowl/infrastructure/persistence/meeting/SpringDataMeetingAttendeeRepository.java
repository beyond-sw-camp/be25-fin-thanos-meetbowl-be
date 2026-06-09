package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataMeetingAttendeeRepository
        extends JpaRepository<MeetingAttendeeEntity, UUID> {

    List<MeetingAttendeeEntity> findByMeetingId(UUID meetingId);

    List<MeetingAttendeeEntity> findByUserId(UUID userId);

    void deleteByMeetingId(UUID meetingId);
}
