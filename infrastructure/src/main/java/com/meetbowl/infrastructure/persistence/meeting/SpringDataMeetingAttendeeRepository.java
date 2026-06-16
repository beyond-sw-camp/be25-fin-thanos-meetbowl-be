package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.meeting.AttendeeRole;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataMeetingAttendeeRepository
        extends JpaRepository<MeetingAttendeeEntity, UUID> {

    List<MeetingAttendeeEntity> findByMeetingId(UUID meetingId);

    List<MeetingAttendeeEntity> findByUserId(UUID userId);

    /** 회의록 검토자 연동용: 회의의 특정 역할(REVIEWER) 참석자 조회. 회의당 검토자는 최대 1명이다. */
    Optional<MeetingAttendeeEntity> findByMeetingIdAndRole(UUID meetingId, AttendeeRole role);

    void deleteByMeetingId(UUID meetingId);
}
