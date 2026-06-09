package com.meetbowl.domain.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 참가자 접속 세션을 영속화하고 현재 회의 참가자 목록을 구성하기 위한 저장소 계약이다.
 *
 * <p>퇴장 시각을 별도 저장하지 않으므로 현재 접속자 조회는 ParticipantSessionStatus.JOINED 상태를 기준으로 수행한다.
 */
public interface ParticipantSessionRepositoryPort {

    /** 입장 요청 또는 연결 상태가 반영된 참가자 세션을 저장한다. */
    ParticipantSession save(ParticipantSession participantSession);

    /** STT 발화자 연결이나 참가자 상태 변경에 사용할 접속 세션을 내부 ID로 조회한다. */
    Optional<ParticipantSession> findById(UUID participantSessionId);

    /** 지정한 회의에서 현재 JOINED 상태인 참가자만 반환한다. */
    List<ParticipantSession> findJoinedByMeetingId(UUID meetingId);
}
