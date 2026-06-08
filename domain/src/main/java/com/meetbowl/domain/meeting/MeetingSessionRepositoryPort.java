package com.meetbowl.domain.meeting;

import java.util.Optional;
import java.util.UUID;

/**
 * 회의 진행 세션 도메인이 필요로 하는 영속화 기능을 정의한다.
 *
 * <p>Application은 이 계약에만 의존하며 JPA Entity나 Spring Data Repository를 알지 못한다. 실제 DB 접근과 Domain/Entity
 * 변환은 Infrastructure Adapter가 담당한다.
 */
public interface MeetingSessionRepositoryPort {

    /** 신규 세션 생성과 상태 변경 결과를 저장하고, DB 식별자가 반영된 도메인 모델을 반환한다. */
    MeetingSession save(MeetingSession meetingSession);

    /** 참가자 세션 등이 참조하는 내부 회의 진행 세션 ID로 단건을 조회한다. */
    Optional<MeetingSession> findById(UUID meetingSessionId);

    /** 상위 회의 ID에 연결된 실시간 진행 세션을 조회한다. 회의당 하나의 세션을 전제로 한다. */
    Optional<MeetingSession> findByMeetingId(UUID meetingId);
}
