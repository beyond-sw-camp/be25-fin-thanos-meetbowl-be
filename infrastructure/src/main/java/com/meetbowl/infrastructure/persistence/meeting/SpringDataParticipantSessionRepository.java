package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.meeting.ParticipantSessionStatus;

/**
 * ParticipantSessionEntity의 CRUD와 회의별 상태 조건 조회를 제공하는 JPA Repository다.
 *
 * <p>파생 쿼리 결과는 Adapter에서 Domain Model로 변환하며 Application 계층에는 이 인터페이스를 노출하지 않는다.
 */
public interface SpringDataParticipantSessionRepository
        extends JpaRepository<ParticipantSessionEntity, UUID> {

    /** 한 실시간 회의 세션에서 지정 상태에 해당하는 참가자 접속 이력을 모두 조회한다. */
    List<ParticipantSessionEntity> findAllByMeetingSessionIdAndStatus(
            UUID meetingSessionId, ParticipantSessionStatus status);
}
