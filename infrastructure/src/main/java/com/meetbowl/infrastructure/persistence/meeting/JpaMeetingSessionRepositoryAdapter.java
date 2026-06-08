package com.meetbowl.infrastructure.persistence.meeting;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.MeetingSession;
import com.meetbowl.domain.meeting.MeetingSessionRepositoryPort;

/**
 * MeetingSessionRepositoryPort를 Spring Data JPA로 구현하는 영속성 Adapter다.
 *
 * <p>Domain과 Application에 JPA Entity가 노출되지 않도록 모든 Domain/Entity 변환을 이 Infrastructure 경계 내부에서 끝낸다.
 */
@Repository
public class JpaMeetingSessionRepositoryAdapter implements MeetingSessionRepositoryPort {

    private final SpringDataMeetingSessionRepository repository;

    /** 실제 SQL 생성과 Entity 영속화를 담당하는 Spring Data Repository를 주입받는다. */
    public JpaMeetingSessionRepositoryAdapter(SpringDataMeetingSessionRepository repository) {
        this.repository = repository;
    }

    /** Domain을 Entity로 변환해 저장한 뒤, 생성된 ID와 DB 값을 반영한 Domain으로 다시 반환한다. */
    @Override
    public MeetingSession save(MeetingSession meetingSession) {
        return repository.save(MeetingSessionEntity.from(meetingSession)).toDomain();
    }

    /** 참가자 세션 등이 참조하는 내부 PK를 사용해 회의 진행 세션을 조회한다. */
    @Override
    public Optional<MeetingSession> findById(UUID meetingSessionId) {
        return repository.findById(meetingSessionId).map(MeetingSessionEntity::toDomain);
    }

    /** 상위 회의 ID에 연결된 실시간 세션을 찾는다. meeting_id unique 제약으로 결과는 최대 하나다. */
    @Override
    public Optional<MeetingSession> findByMeetingId(UUID meetingId) {
        return repository.findByMeetingId(meetingId).map(MeetingSessionEntity::toDomain);
    }
}
