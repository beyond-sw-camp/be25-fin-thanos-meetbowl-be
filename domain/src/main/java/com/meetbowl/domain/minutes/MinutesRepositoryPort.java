package com.meetbowl.domain.minutes;

import java.util.Optional;
import java.util.UUID;

/** domain/application 계층이 회의록 저장소에 기대하는 계약이다. 구현은 infrastructure adapter가 담당한다. */
public interface MinutesRepositoryPort {

    Minutes save(Minutes minutes);

    Optional<Minutes> findById(UUID minutesId);

    Optional<Minutes> findByMeetingId(UUID meetingId);

    boolean existsByMeetingId(UUID meetingId);
}
