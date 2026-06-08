package com.meetbowl.infrastructure.persistence.meeting;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MeetingSessionEntity에 대한 기본 CRUD와 회의 ID 기반 조회를 제공한다.
 *
 * <p>이 인터페이스는 Infrastructure 내부에서만 사용하며 Domain Port 역할을 대신하지 않는다.
 */
public interface SpringDataMeetingSessionRepository
        extends JpaRepository<MeetingSessionEntity, UUID> {

    /** meeting_id unique 제약을 기준으로 상위 회의에 연결된 진행 세션 하나를 조회한다. */
    Optional<MeetingSessionEntity> findByMeetingId(UUID meetingId);
}
