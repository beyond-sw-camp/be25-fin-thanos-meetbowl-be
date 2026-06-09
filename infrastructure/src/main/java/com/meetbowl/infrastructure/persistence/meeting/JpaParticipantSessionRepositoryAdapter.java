package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.ParticipantSession;
import com.meetbowl.domain.meeting.ParticipantSessionRepositoryPort;
import com.meetbowl.domain.meeting.ParticipantSessionStatus;

/**
 * ParticipantSessionRepositoryPort를 Spring Data JPA로 구현하는 영속성 Adapter다.
 *
 * <p>참가자 상태 조회 조건은 Infrastructure에서 Entity 기준 쿼리로 표현하고, 반환 시에는 Domain Model 목록으로 변환한다.
 */
@Repository
public class JpaParticipantSessionRepositoryAdapter implements ParticipantSessionRepositoryPort {

    private final SpringDataParticipantSessionRepository repository;

    /** 참가자 접속 이력의 Entity 저장과 상태 조건 조회를 담당하는 Repository를 주입받는다. */
    public JpaParticipantSessionRepositoryAdapter(
            SpringDataParticipantSessionRepository repository) {
        this.repository = repository;
    }

    /** 참가자 입장 요청 또는 연결 상태 변경 결과를 저장하고 Domain으로 복원한다. */
    @Override
    public ParticipantSession save(ParticipantSession participantSession) {
        return repository.save(ParticipantSessionEntity.from(participantSession)).toDomain();
    }

    /** 참가자 연결 상태 이벤트를 반영할 접속 세션을 내부 PK로 조회한다. */
    @Override
    public Optional<ParticipantSession> findById(UUID participantSessionId) {
        return repository.findById(participantSessionId).map(ParticipantSessionEntity::toDomain);
    }

    /**
     * 지정한 회의에서 현재 JOINED 상태인 참가자만 조회한다.
     *
     * <p>퇴장 시각을 별도로 저장하지 않으므로 현재 참가자 여부는 상태값으로 판단한다.
     */
    @Override
    public List<ParticipantSession> findJoinedByMeetingId(UUID meetingId) {
        return repository
                .findAllByMeetingIdAndStatus(meetingId, ParticipantSessionStatus.JOINED)
                .stream()
                .map(ParticipantSessionEntity::toDomain)
                .toList();
    }
}
